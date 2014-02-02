/*
 * GLSprite
 * 
 * Created on: Apr 26, 2011
 * Author: Dmytro Baryskyy
 */

package com.hackaton.dronedelivery.flight;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

public class GLSprite
{
	private static final int VERTEX_BUFFER = 0;
	private static final int INDEX_BUFFER = 1;
	private static final String TAG = GLSprite.class.getSimpleName();

	private static final int _COUNT = 4;
	private static final int VERTEX_COORDS_SIZE = 3;
	private static final int TEXTURE_COORDS_SIZE = 2;
	private static final int FLOAT_SIZE_BYTES = 4;

	// Left public in order to save method calls
	public int width;
	public int height;

	public int imageWidth;
	public int imageHeight;

	public int textureWidth;
	public int textureHeight;

	public float alpha;

	public Bitmap texture;

	protected boolean readyToDraw;

	private int positionHandle;
	private int textureHandle;
	private int mvpMatrixHandle;
	private int fAlphaHandle;

	protected int[] textures = {
			-1
	};
	protected int[] buffers = {
			-1, -1
	};
	private float[] mMVPMatrix = new float[16];
	private float[] mMMatrix = new float[16];
	private float[] mVMatrix = new float[16];
	private float[] mProjMatrix = new float[16];

	protected int program;
	private Buffer vertices;
	private Buffer indexes;

	private boolean updateVertexBuffer;
	private boolean recalculateMatrix;
	private boolean updateTexture;

	private float prevX;
	private float prevY;

    private boolean mIsVisible = true;
    private boolean mIsCenter = false;

    float mX = 0.0f, mY = 0.0f;

    public void setPosition(float x, float y) {
        mX = x;
        mY = y;
    }
    public void setCenter(boolean value) {
        mIsCenter = value;
    }
    public void show() {
        synchronized (this) {
            mIsVisible = true;
        }
    }
    public void hide() {
        synchronized (this) {
            mIsVisible = false;
        }
    }
    public boolean isVisible() {
        synchronized (this) {
            return mIsVisible;
        }
    }
	public GLSprite() {
		updateVertexBuffer = false;
		recalculateMatrix = true;
		alpha = 1.0f;
		readyToDraw = false;

		texture = Bitmap.createBitmap(32, 32, Bitmap.Config.RGB_565);
		width = 0;
		height = 0;

		imageWidth = width;
		imageHeight = height;

		textureWidth = texture.getWidth();
		textureHeight = texture.getHeight();
	}


	public void init(GL10 gl, int program) {
		this.program = program;

		GLES20.glUseProgram(program);
		checkGlError("glUseProgram program");
		positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
		textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
		mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
		fAlphaHandle = GLES20.glGetUniformLocation(program, "fAlpha");
		checkGlError("glGetAttribLocation");

		recalculateTexturePosition();
	}
	@SuppressLint("NewApi")
	public void recalculateTexturePosition() {
		if (textures[0] != -1) {
			GLES20.glDeleteTextures(1, textures, 0);
		}
		if (buffers[0] != -1) {
			GLES20.glDeleteBuffers(buffers.length, buffers, 0);
		}
		GLES20.glGenTextures(1, textures, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
		checkGlError("glBindTexture");

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
		checkGlError("texImage2D");

		GLES20.glGenBuffers(buffers.length, buffers, 0);
		vertices = createVertex(width, height);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[VERTEX_BUFFER]);
		checkGlError("glBindBuffer buffers[" + VERTEX_BUFFER + "]");
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 20 * FLOAT_SIZE_BYTES, vertices, GLES20.GL_STATIC_DRAW);
		checkGlError("glBufferData vertices");
		GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 0);
		GLES20.glEnableVertexAttribArray(positionHandle);
		GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, 5 * FLOAT_SIZE_BYTES, 3 * FLOAT_SIZE_BYTES);
		GLES20.glEnableVertexAttribArray(textureHandle);
		indexes = createIndex();
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[INDEX_BUFFER]);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 4 * 2, indexes, GLES20.GL_STATIC_DRAW);
	}
	private FloatBuffer createVertex(float width, float height) {
		float texXcoef = (float) imageWidth / (float) textureWidth;
		float texYcoef = (float) imageHeight / (float) textureHeight;
		float[] rectVerticesData = {
				width, 0f, 0f, texXcoef, texYcoef,
				width, height, 0f, texXcoef, 0,
				0, 0, 0, 0, texYcoef,
				0, height, 0, 0, 0
		};
		ByteBuffer vbb = ByteBuffer.allocateDirect(rectVerticesData.length * FLOAT_SIZE_BYTES);
		vbb.order(ByteOrder.nativeOrder());
		FloatBuffer vertices = vbb.asFloatBuffer();
		vertices.put(rectVerticesData);
		vertices.position(0);

		return vertices;
	}
	private ShortBuffer createIndex() {
		short[] indexesData = {
				0, 1, 2, 3
		};
		ByteBuffer vbb = ByteBuffer.allocateDirect(indexesData.length * (Short.SIZE / 8));
		vbb.order(ByteOrder.nativeOrder());
		ShortBuffer indexes = vbb.asShortBuffer();
		indexes.put(indexesData);
		indexes.position(0);
		return indexes;
	}
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		vertices = createVertex(width, height);
		updateVertexBuffer = true;
	}
	protected void onUpdateTexture() {
		if (updateTexture) {
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
			updateTexture = false;

			vertices = createVertex(width, height);
			updateVertexBuffer = true;
		}
	}
	public void setViewAndProjectionMatrices(float[] vMatrix, float[] projMatrix) {
		this.mVMatrix = vMatrix;
		this.mProjMatrix = projMatrix;
		recalculateMatrix = true;
		readyToDraw = true;
	}
	public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (mIsCenter) {
            setSize(width >> 1, height >> 1);
            setPosition((width - this.width) >> 1, (height - this.height) >> 1);
        }
		recalculateMatrix = true;
	}
	@SuppressLint("NewApi")
	public void onDraw(GL10 gl, float x, float y) {
		if (readyToDraw) {
			if (prevX != x || prevY != y) {
				recalculateMatrix = true;
				prevX = x;
				prevY = y;
			}

			GLES20.glUseProgram(program);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

			onUpdateTexture();

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[VERTEX_BUFFER]);

			if (updateVertexBuffer) {
				GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, 20 * FLOAT_SIZE_BYTES, vertices);
				updateVertexBuffer = false;
			}

			int stride = 5 * FLOAT_SIZE_BYTES;            
			GLES20.glVertexAttribPointer(positionHandle, VERTEX_COORDS_SIZE, GLES20.GL_FLOAT, false, stride, 0);
			GLES20.glVertexAttribPointer(textureHandle, TEXTURE_COORDS_SIZE, GLES20.GL_FLOAT, false, stride, VERTEX_COORDS_SIZE * FLOAT_SIZE_BYTES);

			if (recalculateMatrix) {
				Matrix.setIdentityM(mMMatrix, 0);
				Matrix.translateM(mMMatrix, 0, x, y, 0);
				Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
				Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

				recalculateMatrix = false;
			}

			GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mMVPMatrix, 0);

			if (alpha < 1.0f) {
				GLES20.glUniform1f(fAlphaHandle, alpha);
			} else {
				GLES20.glUniform1f(fAlphaHandle, 1.0f);
			}

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[INDEX_BUFFER]);

			GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, _COUNT, GLES20.GL_UNSIGNED_SHORT, 0);

			checkGlError("glDrawElements");
		}
	}
	public void setAlpha(float alpha) {
		if (alpha > 1.0f)
			this.alpha = 1.0f;

		if (alpha < 0.0f) {
			this.alpha = 0;
		}

		this.alpha = alpha;
	}
	public void updateTexture(Bitmap bitmap) {
		if (texture != null) {
			texture.recycle();
		}
		texture = makeTexture(bitmap);
		width = bitmap.getWidth();
		height = bitmap.getHeight();
		imageWidth = width;
		imageHeight = height;
		textureWidth = texture.getWidth();
		textureHeight = texture.getHeight();
		updateTexture = true;
	}
	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			try {
				throw new RuntimeException(op + ": glError " + error);
			} catch (RuntimeException e) {
				Log.w(TAG, Log.getStackTraceString(e));
			}
		}
	}
	public void freeResources() {
		if (texture != null) {
			texture.recycle();
		}
	}
	public boolean isReadyToDraw() {
		return readyToDraw;
	}
	public static long roundPower2(final long x) {
		int rval = 256;
		while(rval < x) {
			rval <<= 1;
		}
		return rval;
	}
	public static Bitmap makeTexture(Bitmap bmp) {
		if (bmp == null) {
			throw new IllegalArgumentException("Bitmap can't be null");
		}
		int height = (int) roundPower2(bmp.getHeight());
		int width = (int) roundPower2(bmp.getWidth());
		Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(result);
		canvas.drawBitmap(bmp, 0, 0, null);
		return result;
	}
}
