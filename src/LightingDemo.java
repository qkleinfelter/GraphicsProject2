
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import static com.jogamp.opengl.GL4.GL_MAP_COHERENT_BIT;
import static com.jogamp.opengl.GL4.GL_MAP_PERSISTENT_BIT;

/**
 * 
 */

/**
 * @author Jerry Heuring
 *
 *         9/20/2021 : Updated to do 3 views and rotation using arrow keys.
 *         9/20/2021 : Fixed error in call to glDrawArrays() -- number of
 *         vertices was incorrect.
 * 
 */
public class LightingDemo {
	private interface Buffer {

		int VERTEX = 0;
		int ELEMENT = 1;
		int GLOBAL_MATRICES = 2;
		int MODEL_MATRIX = 3;
		int MAX = 4;
	}

	/**
	 * Created by GBarbieri on 16.03.2017.
	 * 
	 * Program heavily modified by Jerry Heuring in September 2021. Most
	 * modifications stripped out code that was not yet needed reorganized the
	 * remaining code to more closely align with the C/C++ version of the initial
	 * program.
	 */
	public class HelloTriangleSimple implements GLEventListener, KeyListener {

		private GLWindow window;
		private Animator animator;
		private boolean continuous = false;
		private float time = 0.0f;
		private float prevX = 0.0f, prevZ = 0.0f;

		public void main(String[] args) {
			new HelloTriangleSimple().setup();
		}

		private int nbrVertices;
		private final IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
		private final IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(3);
		private Program program;
		private long start;
		private final PMVMatrix rotationMatrix = new PMVMatrix();
		private final PMVMatrix viewMatrix = new PMVMatrix();
		private final PMVMatrix projectionMatrix = new PMVMatrix();
		private final float[][] instanceOffsets = new float[10][3];
		private boolean useInstanced = false;

		private void setup() {

			GLProfile glProfile = GLProfile.get(GLProfile.GL4);
			GLCapabilities glCapabilities = new GLCapabilities(glProfile);

			window = GLWindow.create(glCapabilities);

			window.setTitle("Perspective Demo");
			window.setSize(600, 600);

			window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
			window.setVisible(true);

			window.addGLEventListener(this);
			window.addKeyListener(this);

			animator = new Animator(window);
			animator.start();

			window.addWindowListener(new WindowAdapter() {
				@Override
				public void windowDestroyed(WindowEvent e) {
					animator.stop();
					System.exit(1);
				}
			});
		}

		@Override
		public void init(GLAutoDrawable drawable) {

			GL4 gl = drawable.getGL().getGL4();

			initDebug(gl);
			program = new Program(gl, "src/", "passthrough", "directional");
			rotationMatrix.glLoadIdentity();
			viewMatrix.glLoadIdentity();
			projectionMatrix.glLoadIdentity();
			projectionMatrix.glOrthof(-10.0f, 10.0f, -10.0f, 10.0f, -100f, 100f);
			buildObjects(gl);
			gl.glEnable(GL_DEPTH_TEST);
			gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
			start = System.currentTimeMillis();
		}

		private void initDebug(GL4 gl) {

			window.getContext().addGLDebugListener(System.out::println);
			/*
			 * sets up medium and high severity error messages to be printed.
			 */
			gl.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, null, false);

			gl.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_HIGH, 0, null, true);

			gl.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_MEDIUM, 0, null, true);
		}

		private void buildObjects(GL4 gl) {
			OBJinfo obj = new OBJinfo();
			obj.readOBJFile("objects/elephantTriangles.obj");

			FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(obj.getVertexList());
			FloatBuffer normalBuffer = GLBuffers.newDirectFloatBuffer(obj.getNormalList());

			System.out.println("vertexBuffer Capacity = " + vertexBuffer.capacity() + "  normalBuffer Capacity = " + normalBuffer.capacity());
			// build 3 vertex array objects
			gl.glGenVertexArrays(3, vertexArrayName);
			// bind the first VAO
			gl.glBindVertexArray(vertexArrayName.get(0));
			// gen buffer array object
			gl.glGenBuffers(Buffer.MAX, bufferName);
			// bind buffer array object
			gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
			// create the buffer & sub buffer data for this BAO
			gl.glBufferData(GL_ARRAY_BUFFER, (vertexBuffer.capacity() * 4L + normalBuffer.capacity()) * 3, null,
					GL_STATIC_DRAW);
			gl.glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexBuffer.capacity() * 4L, vertexBuffer);
			gl.glBufferSubData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4L, normalBuffer.capacity() * 4L, normalBuffer);
			nbrVertices = vertexBuffer.capacity() / 4;
			int vPosition = gl.glGetAttribLocation(program.name, "vPosition");
			int vNormal = gl.glGetAttribLocation(program.name, "vNormal");
			gl.glEnableVertexAttribArray(vPosition);
			gl.glVertexAttribPointer(vPosition, 4, GL_FLOAT, false, 0, 0);
			if (vNormal != -1){
				gl.glEnableVertexAttribArray(vNormal);
				gl.glVertexAttribPointer(vNormal, 3, GL_FLOAT, false, 0, vertexBuffer.capacity() * 4L);
			}

			OBJinfo cylinder = new OBJinfo();
			cylinder.readOBJFile("objects/cylinderProject2.obj");
			vertexBuffer = GLBuffers.newDirectFloatBuffer(cylinder.getVertexList());
			normalBuffer = GLBuffers.newDirectFloatBuffer(cylinder.getNormalList());
			System.out.println("vertexBuffer Capacity = " + vertexBuffer.capacity() + "  normalBuffer Capacity = " + normalBuffer.capacity());

			// bind the second VAO
			gl.glBindVertexArray(vertexArrayName.get(1));
			// gen buffer array object
			gl.glGenBuffers(Buffer.MAX, bufferName);
			// create the buffer & sub buffer data for this BAO
			gl.glBufferData(GL_ARRAY_BUFFER, (vertexBuffer.capacity() * 4L + normalBuffer.capacity()) * 3, null, GL_STATIC_DRAW);
			gl.glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexBuffer.capacity() * 4L, vertexBuffer);
			gl.glBufferSubData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4L, normalBuffer.capacity() * 4L, normalBuffer);
			nbrVertices += vertexBuffer.capacity() / 4;

			OBJinfo cone = new OBJinfo();
			cone.readOBJFile("objects/coneProject2.obj");
			vertexBuffer = GLBuffers.newDirectFloatBuffer(cone.getVertexList());
			normalBuffer = GLBuffers.newDirectFloatBuffer(cone.getNormalList());
			System.out.println("vertexBuffer Capacity = " + vertexBuffer.capacity() + "  normalBuffer Capacity = " + normalBuffer.capacity());

			// bind the third VAO
			gl.glBindVertexArray(vertexArrayName.get(2));
			// gen buffer array object
			gl.glGenBuffers(Buffer.MAX, bufferName);
			// create the buffer & sub buffer data for this BAO
			gl.glBufferData(GL_ARRAY_BUFFER, (vertexBuffer.capacity() * 4L + normalBuffer.capacity()) * 3, null, GL_STATIC_DRAW);
			gl.glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexBuffer.capacity() * 4L, vertexBuffer);
			gl.glBufferSubData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4L, normalBuffer.capacity() * 4L, normalBuffer);
			nbrVertices += vertexBuffer.capacity() / 4;
		}

		@Override
		/*
		 * Display the object. One issue with this is that it has the number of
		 * triangles hardcoded at the moment -- hopefully I will fix this so that it
		 * comes from the buffer or some other reasonable object. (non-Javadoc)
		 * 
		 * @see
		 * com.jogamp.opengl.GLEventListener#display(com.jogamp.opengl.GLAutoDrawable)
		 */
		public void display(GLAutoDrawable drawable) {

			GL4 gl = drawable.getGL().getGL4();

			gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			gl.glUseProgram(program.name);
			setupDirectedLights(gl);
			
			gl.glBindVertexArray(vertexArrayName.get(0));
			gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
			int modelMatrixLocation = gl.glGetUniformLocation(program.name, "modelingMatrix");
			gl.glUniformMatrix4fv(modelMatrixLocation, 1, false, rotationMatrix.glGetMatrixf());
			int viewMatrixLocation = gl.glGetUniformLocation(program.name, "viewingMatrix");
			gl.glUniformMatrix4fv(viewMatrixLocation, 1, false, viewMatrix.glGetMatrixf());
			int projectionMatrixLocation = gl.glGetUniformLocation(program.name, "projectionMatrix");
			gl.glUniformMatrix4fv(projectionMatrixLocation, 1, false, projectionMatrix.glGetMatrixf());
			int normalMatrixLocation = gl.glGetUniformLocation(program.name, "normalMatrix");
			PMVMatrix scale = new PMVMatrix();
			scale.glScalef(1.0f, 1.0f, 1.0f);
			PMVMatrix trsMatrix = new PMVMatrix();
			trsMatrix.glLoadIdentity();
			trsMatrix.glMultMatrixf(rotationMatrix.glGetMatrixf());
			trsMatrix.glMultMatrixf(scale.glGetMatrixf());
			gl.glUniformMatrix4fv(normalMatrixLocation,  1,  false, trsMatrix.glGetMvitMatrixf());
			gl.glUniformMatrix4fv(modelMatrixLocation, 1, false, trsMatrix.glGetMatrixf());
			gl.glDrawArrays(GL_TRIANGLES, 0, nbrVertices);

			if (continuous) {
				moveAlongEquation();
			}
		}

		/**
		 * This method sets up the lighting information for the directed lights
		 * for this application.  
		 * 
		 * @param gl  -- opengl context
		 */
		private void setupDirectedLights(GL4 gl) {
			int ambientLightLocation = gl.glGetUniformLocation(program.name, "ambientLight");
			int lightDirectionLocation = gl.glGetUniformLocation(program.name, "lightDirection");
			int lightColorLocation = gl.glGetUniformLocation(program.name, "lightColor");
			int shininessLocation = gl.glGetUniformLocation(program.name, "shininess");
			int strengthLocation = gl.glGetUniformLocation(program.name, "strength");
			int halfVectorLocation = gl.glGetUniformLocation(program.name, "halfVector");
			float[] ambientLight = {0.4f, 0.4f, 0.4f};
			float[] lightDirection = {0.0f, 0.7071f, 0.7071f};
			float[] lightColor = { 0.5f, 0.5f, 0.5f };
			float[] halfVector = {0.0f, 0.45514f, 0.9240f };
			float strength = 1.0f;
			float shininess = 25.0f;
			gl.glUniform1f(shininessLocation, shininess);
			gl.glUniform1f(strengthLocation, strength);
			gl.glUniform3f(halfVectorLocation, halfVector[0], halfVector[1], halfVector[2]);
			gl.glUniform3f(lightColorLocation, lightColor[0], lightColor[1], lightColor[2]);
			gl.glUniform3f(lightDirectionLocation, lightDirection[0], lightDirection[1], lightDirection[2]);
			gl.glUniform3f(ambientLightLocation, ambientLight[0], ambientLight[1], ambientLight[2]);
		}

		@Override
		/*
		 * handles window reshapes -- it should affect the size of the view as well so
		 * that things remain square but since we haven't gotten to projections yet it
		 * does not.
		 * 
		 * @see
		 * com.jogamp.opengl.GLEventListener#reshape(com.jogamp.opengl.GLAutoDrawable,
		 * int, int, int, int)
		 */
		public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

			GL4 gl = drawable.getGL().getGL4();
			gl.glViewport(x, y, width, height);
		}

		@Override
		/*
		 * This method disposes of resources cleaning up at the end. This wasn't
		 * happening in the C/C++ version but would be a good idea.
		 */
		public void dispose(GLAutoDrawable drawable) {
			GL4 gl = drawable.getGL().getGL4();

			gl.glDeleteProgram(program.name);
			gl.glDeleteVertexArrays(1, vertexArrayName);
			gl.glDeleteBuffers(Buffer.MAX, bufferName);
		}

		@Override
		/*
		 * Keypress callback for java -- handle a keypress (non-Javadoc)
		 * 
		 * @see
		 * com.jogamp.newt.event.KeyListener#keyPressed(com.jogamp.newt.event.KeyEvent)
		 */
		public void keyPressed(KeyEvent e) {
			short keyCode = e.getKeyCode();
			if (keyCode == KeyEvent.VK_ESCAPE) {
				new Thread(() -> {
					window.destroy();
				}).start();
			} else if (keyCode == KeyEvent.VK_RIGHT) {
				rotationMatrix.glRotatef(10.0f, 0.0f, 1.0f, 0.0f);
			} else if (keyCode == KeyEvent.VK_LEFT) {
				rotationMatrix.glRotatef(-10.0f, 0.0f, 1.0f, 0.0f);
			} else if (keyCode == KeyEvent.VK_UP) {
				rotationMatrix.glRotatef(10.0f, 1.0f, 0.0f, 0.0f);
			} else if (keyCode == KeyEvent.VK_DOWN) {
				rotationMatrix.glRotatef(-10.0f, 1.0f, 0.0f, 0.0f);
			}else if (keyCode == KeyEvent.VK_X) {
				viewMatrix.glLoadIdentity();
				viewMatrix.gluLookAt(25.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
			} else if (keyCode == KeyEvent.VK_Z) {
				viewMatrix.glLoadIdentity();
				viewMatrix.gluLookAt(0.0f, 0.0f, 25.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
			} else if (keyCode == KeyEvent.VK_Y) {
				viewMatrix.glLoadIdentity();
				viewMatrix.gluLookAt(0.0f, 25.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
			} else if (keyCode == KeyEvent.VK_O) {
				projectionMatrix.glLoadIdentity();
				projectionMatrix.glOrthof(-10f, 10f, -10f, 10f, -100f, 100f);
			} else if (keyCode == KeyEvent.VK_P) {
				projectionMatrix.glLoadIdentity();
				projectionMatrix.gluPerspective(60.0f, 1.0f, 0.01f, 1000.0f);
			} else if (keyCode == KeyEvent.VK_C) {
				continuous = true;
				System.out.println("Activated continuous movement");
			} else if (keyCode == KeyEvent.VK_S) {
				if (!continuous) {
					// do one step
					moveAlongEquation();
					System.out.println("Moved 1 step");
				} else {
					// otherwise, stop running in continuous mode
					continuous = false;
					System.out.println("Deactivated continuous movement");
				}
			}
		}

		private void moveAlongEquation() {
			time += 0.01f;
			float x = (float) (5 * Math.sin(time + (Math.PI / 2)));
			float z = (float) (5 * Math.sin(2 * time));
			float xMovement = x - prevX;
			float zMovement = z - prevZ;
			rotationMatrix.glTranslatef(xMovement, 0, zMovement);
			prevX = x;
			prevZ = z;
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		/*
		 * private class to handle building the shader program from filenames. This one
		 * is different from the C/C++ one in that it does not take the complete path.
		 * It has a path and a file name and then insists on the extensions .vert,
		 * .frag.
		 * 
		 * I think we will rewrite this one to do a few other things before the class is
		 * over. Right now it works.
		 */
		private class Program {

			public int name = 0;

			public Program(GL4 gl, String root, String vertex, String fragment) {

				ShaderCode vertShader = ShaderCode.create(gl, GL_VERTEX_SHADER, this.getClass(), root, null, vertex,
						"vert", null, true);
				ShaderCode fragShader = ShaderCode.create(gl, GL_FRAGMENT_SHADER, this.getClass(), root, null, fragment,
						"frag", null, true);

				ShaderProgram shaderProgram = new ShaderProgram();

				shaderProgram.add(vertShader);
				shaderProgram.add(fragShader);

				shaderProgram.init(gl);

				name = shaderProgram.program();

				shaderProgram.link(gl, System.err);
			}
		}

		/*
		 * Class to set up debug output from OpenGL. Again, I haven't done this in the
		 * C/C++ version but it would be a good idea.
		 */
		private class GlDebugOutput implements GLDebugListener {

			private int source = 0;
			private int type = 0;
			private int id = 0;
			private int severity = 0;
			private int length = 0;
			private String message = null;
			private boolean received = false;

			public GlDebugOutput() {
			}

			public GlDebugOutput(int source, int type, int severity) {
				this.source = source;
				this.type = type;
				this.severity = severity;
				this.message = null;
				this.id = -1;
			}

			public GlDebugOutput(String message, int id) {
				this.source = -1;
				this.type = -1;
				this.severity = -1;
				this.message = message;
				this.id = id;
			}

			@Override
			public void messageSent(GLDebugMessage event) {

				if (event.getDbgSeverity() == GL_DEBUG_SEVERITY_LOW
						|| event.getDbgSeverity() == GL_DEBUG_SEVERITY_NOTIFICATION)
					System.out.println("GlDebugOutput.messageSent(): " + event);
				else
					System.err.println("GlDebugOutput.messageSent(): " + event);

				if (null != message && message.equals(event.getDbgMsg()) && id == event.getDbgId())
					received = true;
				else if (0 <= source && source == event.getDbgSource() && type == event.getDbgType()
						&& severity == event.getDbgSeverity())
					received = true;
			}
		}
	}

	/**
	 * Default constructor for the class does nothing in this case. It simply gives
	 * a starting point to create an instance and then run the main program from the
	 * class.
	 */
	public LightingDemo() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LightingDemo myInstance = new LightingDemo();
		HelloTriangleSimple example = myInstance.new HelloTriangleSimple();
		example.main(args);
	}

}
