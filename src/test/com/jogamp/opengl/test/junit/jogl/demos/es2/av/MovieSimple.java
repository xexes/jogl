/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */


package com.jogamp.opengl.test.junit.jogl.demos.es2.av;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLES2;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.av.GLMediaPlayer;
import com.jogamp.opengl.av.GLMediaEventListener;
import com.jogamp.opengl.av.GLMediaPlayer.TextureFrame;
import com.jogamp.opengl.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;

public class MovieSimple implements MouseListener, GLEventListener, GLMediaEventListener {
    private boolean quit = false;
    private int winWidth, winHeight;
    private int prevMouseX; // , prevMouseY;
    private boolean rotate = false;
    private float zoom = -2.5f;
    private float ang = 0f;
    private long startTime;
    private long curTime;
    private int effects = EFFECT_NORMAL;
    private float alpha = 1.0f;
    private int texUnit = 0;

    public static final int EFFECT_NORMAL      =    0;
    public static final int EFFECT_GRADIENT_BOTTOM2TOP     = 1<<1;
    public static final int EFFECT_TRANSPARENT = 1<<3; 

    public void setTextureUnit(int u) { texUnit = u; }
    public int getTextureUnit() { return texUnit; }
    
    public boolean hasEffect(int e) { return 0 != ( effects & e ) ; }
    public void setEffects(int e) { effects = e; };
    public void setTransparency(float alpha) {
        this.effects |= EFFECT_TRANSPARENT;
        this.alpha = alpha;
    }
    
    public void changedAttributes(GLMediaPlayer omx, int event_mask) {
        System.out.println("changed stream attr ("+event_mask+"): "+omx);
    }

    public void mouseClicked(MouseEvent e) {
        switch(e.getClickCount()) {
            case 2:
                quit=true;
                break;
        }
    }
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
    public void mousePressed(MouseEvent e) {
        if(e.getY()<=winHeight/2 && null!=mPlayer && 1 == e.getClickCount()) {
            if(GLMediaPlayer.State.Playing == mPlayer.getState()) {
                mPlayer.pause();
            } else {
                mPlayer.start();
            }
        }
    }
    public void mouseReleased(MouseEvent e) {
        if(e.getY()<=winHeight/2) {
            rotate = false;
            zoom = -2.5f;
        }
    }
    public void mouseMoved(MouseEvent e) {
        prevMouseX = e.getX();
        // prevMouseY = e.getY();
    }
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        
        if(y>winHeight/2) {
            final float dp  = (float)(x-prevMouseX)/(float)winWidth;
            mPlayer.seek(mPlayer.getCurrentPosition() + (long) (mPlayer.getDuration() * dp));                
        } else {
            mPlayer.start();
            rotate = true;                
            zoom = -5;
        }
        
        prevMouseX = x;
        // prevMouseY = y;
    }
    public void mouseWheelMoved(MouseEvent e) {
    }

    GLMediaPlayer mPlayer;
    boolean mPlayerExternal;
    boolean mPlayerShared;
    boolean mPlayerScaleOrig;

    public MovieSimple(URL stream) throws IOException {
        mPlayerScaleOrig = false;
        mPlayerExternal = false;
        mPlayer = GLMediaPlayerFactory.create();
        mPlayer.addEventListener(this);
        mPlayer.initStream(stream);        
        System.out.println("p0.1 "+mPlayer);
    }

    public MovieSimple(GLMediaPlayer mediaPlayer, boolean shared) throws IllegalStateException {
        if(!shared && GLMediaPlayer.State.UninitializedGL != mediaPlayer.getState()) {
            throw new IllegalStateException("Given GLMediaPlayer not in state "+GLMediaPlayer.State.UninitializedGL+": "+mediaPlayer);
        }
        mPlayerScaleOrig = false;
        mPlayerShared = shared;
        mPlayerExternal = true;
        mPlayer = mediaPlayer;
        mPlayer.addEventListener(this);
        System.out.println("p0.2 shared "+mPlayerShared+", "+mPlayer);
    }
    
    public void setScaleOrig(boolean v) {
        mPlayerScaleOrig = v;
    }
    
    private void run() {
        System.err.println("MovieSimple.run()");
        try {
            GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
            GLWindow window = GLWindow.create(caps);

            window.addGLEventListener(this);

            // Size OpenGL to Video Surface
            window.setFullscreen(true);
            window.setVisible(true);

            while (!quit) {
                window.display();
            }

            // Shut things down cooperatively
            if(null!=mPlayer) {
                mPlayer.destroy(window.getGL());
                mPlayer=null;
            }
            window.destroy();
            System.out.println("MovieSimple shut down cleanly.");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    ShaderState st;
    PMVMatrix pmvMatrix;
    GLUniformData pmvMatrixUniform;
    
    private void initShader(GL2ES2 gl, boolean useExternalTexture) {
// Create & Compile the shader objects
        final String vShaderBasename = gl.isGLES2() ? "moviesimple_es2" : "moviesimple_gl2" ;
        final String fShaderBasename = gl.isGLES2() ? ( useExternalTexture ? "moviesimple_es2_exttex" : "moviesimple_es2" ) : "moviesimple_gl2";
        
        ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, MovieSimple.class,
                                            "../shader", "../shader/bin", vShaderBasename);
        ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, MovieSimple.class,
                                            "../shader", "../shader/bin", fShaderBasename);

        // Create & Link the shader program
        ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        // Let's manage all our states using ShaderState.
        st = new ShaderState();
        st.attachShaderProgram(gl, sp, false);
    }

    public void init(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println(JoglVersion.getGLInfo(gl, null));
        System.err.println("Alpha: "+alpha+", opaque "+drawable.getChosenGLCapabilities().isBackgroundOpaque()+
                           ", "+drawable.getClass().getName()+", "+drawable);

        boolean useExternalTexture = false;
        try {
            if(!mPlayerShared) {
                mPlayer.initGL(gl);
            }
            System.out.println("p1 "+mPlayer);
            useExternalTexture = GLES2.GL_TEXTURE_EXTERNAL_OES == mPlayer.getTextureTarget();
            if(useExternalTexture && !gl.isExtensionAvailable("GL_OES_EGL_image_external")) {
                throw new GLException("GL_OES_EGL_image_external requested but not available");
            }
            if(!mPlayerShared) {
                mPlayer.setTextureMinMagFilter( new int[] { GL.GL_NEAREST, GL.GL_LINEAR } );
            }
        } catch (GLException glex) { 
            if(!mPlayerShared && null != mPlayer) {
                mPlayer.destroy(gl);
                mPlayer = null;
            }
            throw new GLException(glex);
        }
        
        pmvMatrix = new PMVMatrix();

        initShader(gl, useExternalTexture);

        // Push the 1st uniform down the path 
        st.useProgram(gl, true);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();

        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        if(!st.uniform(gl, pmvMatrixUniform)) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }
        final GLMediaPlayer.TextureFrame texFrame = mPlayer.getLastTexture();
        if(!st.uniform(gl, new GLUniformData("mgl_ActiveTexture", texUnit))) {
            throw new GLException("Error setting mgl_ActiveTexture in shader: "+st);
        }
        
        float dWidth = drawable.getWidth();
        float dHeight = drawable.getHeight();
        float mWidth = mPlayer.getWidth();
        float mHeight = mPlayer.getHeight();        
        float aspect = mWidth/mHeight;
        float xoff, yoff;
        float xs, ys; // scale object
        float ss, ts; // scale tex-coord
        if(mPlayerScaleOrig && mWidth < dWidth && mHeight < dHeight) {
            xoff = ((dWidth-mWidth)/2f)/dWidth;  yoff = ((dHeight-mHeight)/2f)/dHeight;
            xs   = aspect * ( mWidth / dWidth ); ys   =   mHeight/dHeight;
            ss   = 1f;                           ts   = 1f;
        } else {
            xoff = 0f;     yoff = 0f;
            xs   = aspect; ys   = 1f; // b>h
            ss   = 1f;     ts   = 1f; // b>h
        }

        final GLArrayDataServer interleaved = GLArrayDataServer.createGLSLInterleaved(9, GL.GL_FLOAT, false, 12, GL.GL_STATIC_DRAW);
        {        
            GLArrayData vertices = interleaved.addGLSLSubArray("mgl_Vertex", 3, GL.GL_ARRAY_BUFFER);
            FloatBuffer verticeb = (FloatBuffer)vertices.getBuffer();
            
            GLArrayData texcoord = interleaved.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);
            TextureCoords tc = texFrame.getTexture().getImageTexCoords();
            FloatBuffer texcoordb = (FloatBuffer)texcoord.getBuffer();
            
            GLArrayData colors = interleaved.addGLSLSubArray("mgl_Color",  4, GL.GL_ARRAY_BUFFER);
            FloatBuffer colorb = (FloatBuffer)colors.getBuffer();
            
             // left-bottom
            verticeb.put(-1f*xs+xoff);  verticeb.put( -1f*ys+yoff);  verticeb.put( 0);
            texcoordb.put( tc.left()   *ss);  texcoordb.put( tc.bottom() *ts);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                colorb.put( 0);    colorb.put( 0);     colorb.put( 0);    colorb.put(alpha);
            } else {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            }
            
             // right-bottom
            verticeb.put( 1f*xs+xoff);  verticeb.put( -1f*ys+yoff);  verticeb.put( 0);
            texcoordb.put( tc.right()  *ss);  texcoordb.put( tc.bottom() *ts);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                colorb.put( 0);    colorb.put( 0);     colorb.put( 0);    colorb.put(alpha); 
            } else {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            }

             // left-top
            verticeb.put(-1f*xs+xoff);  verticeb.put(  1f*ys+yoff);  verticeb.put( 0);
            texcoordb.put( tc.left()   *ss);  texcoordb.put( tc.top()    *ts);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            } else {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            }
            
             // right-top
            verticeb.put( 1f*xs+xoff);  verticeb.put(  1f*ys+yoff);  verticeb.put( 0);
            texcoordb.put( tc.right()  *ss);  texcoordb.put( tc.top()    *ts);            
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            } else {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            }
        }
        interleaved.seal(gl, true);
        
        // OpenGL Render Settings
        gl.glClearColor(0f, 0f, 0f, 0f);
        
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);

        st.useProgram(gl, false);

        // Let's show the completed shader state ..
        System.out.println(st);

        if(null!=mPlayer) {
            start();
            System.out.println("p1 "+mPlayer);
        }
        
        startTime = System.currentTimeMillis();
        
        if (drawable instanceof Window) {
            Window window = (Window) drawable;
            window.addMouseListener(this);
            winWidth = window.getWidth();
            winHeight = window.getHeight();
        }
    }
    
    public void start() {
        if(null!=mPlayer) {
            mPlayer.start();
            System.out.println("pStart "+mPlayer);
        }        
    }

    public void stop() {
        if(null!=mPlayer) {
            mPlayer.stop();
            System.out.println("pStop "+mPlayer);
        }        
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        if(null == mPlayer) { return; }
        winWidth = width;
        winHeight = height;
        
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.useProgram(gl, true);

        // Set location in front of camera
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);
        //pmvMatrix.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, zoom);
        st.uniform(gl, pmvMatrixUniform);

        st.useProgram(gl, false);
        
        System.out.println("p2 "+mPlayer);
    }

    public void dispose(GLAutoDrawable drawable) {
        if(null == mPlayer) { return; }
        
        stop();
        
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        mPlayer.removeEventListener(this);
        if(!mPlayerExternal) {
            mPlayer.destroy(gl);
        }
        mPlayer=null;
        pmvMatrixUniform = null;
        pmvMatrix.destroy();
        pmvMatrix=null;
        st.destroy(gl);
        st=null;
        quit=true;
    }

    public void display(GLAutoDrawable drawable) {
        if(null == mPlayer) { return; }
        
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        st.useProgram(gl, true);

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if(rotate) {
            curTime = System.currentTimeMillis();
            ang = ((float) (curTime - startTime) * 360.0f) / 8000.0f;
        }

        if(rotate || zoom!=0f) {
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glTranslatef(0, 0, zoom);
            pmvMatrix.glRotatef(ang, 0, 0, 1);
            st.uniform(gl, pmvMatrixUniform);

            if(!rotate) {
                zoom=0f;
            }
        }

        final Texture tex;
        if(null!=mPlayer) {
            final GLMediaPlayer.TextureFrame texFrame;
            if(mPlayerShared) {
                texFrame=mPlayer.getLastTexture();
            } else {
                texFrame=mPlayer.getNextTexture();
            }
            tex = texFrame.getTexture();
            gl.glActiveTexture(GL.GL_TEXTURE0+texUnit);
            tex.enable(gl);
            tex.bind(gl);
        } else {
            tex = null;
        }

        // Draw a square
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);

        if(null!=tex) {
            tex.disable(gl);
        }

        st.useProgram(gl, false);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    public static void main(String[] args) throws IOException, MalformedURLException {
        String fname="file:///mnt/sdcard/Movies/BigBuckBunny_320x180.mp4";
        if(args.length>0) fname=args[0];
        new MovieSimple(new URL(fname)).run();
        System.exit(0);
    }

    @Override
    public void attributesChanges(GLMediaPlayer mp, int event_mask) {
        System.out.println("attributesChanges: "+mp+", 0x"+Integer.toHexString(event_mask));        
    }

    @Override
    public void newFrameAvailable(GLMediaPlayer mp, TextureFrame frame) {
        // System.out.println("newFrameAvailable: "+mp+", "+frame);                
    }
}