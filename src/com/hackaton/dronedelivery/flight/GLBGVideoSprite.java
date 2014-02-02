package com.hackaton.dronedelivery.flight;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLES20;

public class GLBGVideoSprite extends GLSprite {
	private android.graphics.Matrix matrix;
	private Object videoFrameLock;
	private Bitmap video;
	private float videoSize[]  = {0,0, 0, 0};
	public int screenWidth;
	public int screenHeight;
	private int videoWidth;
	private int videoHeight;
	private boolean isVideoReady = false;
	
	private int prevImgWidth;
	private int prevImgHeight;
	
	private int x;
	private int y;
	
	
	public GLBGVideoSprite() {
		super();
		videoFrameLock = new Object();
		video = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
	}
	@Override
	protected void onUpdateTexture() {
		if (onUpdateVideoTextureNative(program, textures[0]) && (prevImgWidth != imageWidth || prevImgHeight != imageHeight)) {
		    if (!isVideoReady) {
		        isVideoReady = true;
		    }
		    
			float coef = ((float)screenWidth / (float)imageWidth);
			
			setSize((int)(imageWidth * coef), (int)(imageHeight * coef));
			x = (screenWidth - width) / 2;
			y = (screenHeight - height) / 2;
			
			prevImgWidth = imageWidth;
			prevImgHeight = imageHeight;
		}
	}
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		screenWidth = width;
		screenHeight = height;
		onSurfaceChangedNative(width, height);
		super.onSurfaceChanged(gl, width, height);
	}
	@Override
	public void onDraw(GL10 gl, float x, float y) {
		if (!isVideoReady) {
	        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
	    }
		super.onDraw(gl, this.x, this.y);
	}
	public boolean updateVideoFrame() {
		boolean success = false;
		synchronized (videoFrameLock) {
			if (getVideoFrameNative(video, videoSize)) {
				if (matrix == null) {
					matrix = new android.graphics.Matrix();
				}
				float newVideoWidth = videoSize[0];
				float newVideoHeight = videoSize[1];
				if (newVideoWidth != videoWidth || newVideoHeight != videoHeight) {
					videoWidth = (int)newVideoWidth;
					videoHeight = (int)newVideoHeight;
					if (video != null) {
						video.recycle();
					}
					video = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.RGB_565);
					matrix.reset();	
					if (videoWidth != 0 && videoHeight != 0) {
						matrix.setScale((float)screenHeight / (float)videoHeight, (float)screenHeight / (float)videoHeight);
					}
				}		
				isVideoReady = true;
				success = true;
			}			
		}
		return success;
	}
	private native boolean onUpdateVideoTextureNative(int program, int textureId);
	private native void onSurfaceCreatedNative();
	private native void onSurfaceChangedNative(int width, int height);
	private native boolean getVideoFrameNative(Bitmap bitmap, float[] ret);
}
