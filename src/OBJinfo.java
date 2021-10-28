import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * 
 */

/**
 * @author Jerry Heuring
 * 
 * Class for reading in Wavefront OBJ files and
 * converting them to polygon arrays.   This is 
 * still being worked on but seems stable enough
 * to release as a "beta" version.  It has a default 
 * constructor and you can request the vertex array
 * using getVertexList and the normal array using getNormalList.
 * <P>
 * The main routine other than these is the readOBJFile 
 * method that opens and reads the Wavefront OBJ file. 
 * 
 * <P>
 * Bugs:
 *    <ul>
 *    <LI>On my cow model it appears that the normals in the
 *    model are flipped for a portion of the triangles.  
 *    I'm not sure why this is happening yet -- but the
 *    data is there and the vertices are correct.  An
 *    ambient only call produces correct output and I've 
 *    tried to shade triangles with normals that are
 *    pointing away from the light source a different color and
 *    all the triangles show up.  </LI>
 *    
 *    <LI>There is debugging code still in the methods here.  It 
 *    shouldn't cause problems and will eventually be removed. 
 *    </LI>
 *    <LI>
 *    Textures are not processed. 
 *    </LI>
 *    <LI>
 *    The smoothing and object directives are not implemented.
 *    </LI>
 *    </UL>
 *    
 * 
 */
public class OBJinfo {

	private ArrayList<Float> vertices, normals, texture;
	private ArrayList<Integer> triangles;
	private float triangleList[];
	private float normalList[];
	
	/**
	 * Process a string of tokens that represent a vertex. 
	 * the coordinates of the vertex will be added at the 
	 * end of the vertices arrayList.
	 * 
	 * @param tokens  The tokens for the vertex line. 
	 */
	private void processVertex(String[] tokens) {
		if (tokens.length != 4) {
			System.out.println("Vertex without 4 tokens!");
			for (String str:tokens) {
				System.out.println("Token :"+str);
			}
		}
		vertices.add(Float.parseFloat(tokens[1]));
		vertices.add(Float.parseFloat(tokens[2]));
		vertices.add(Float.parseFloat(tokens[3]));
	}

	/**
	 * Process a string of tokens that represent a normal at
	 * a vertex.  The normal will be added to the normals
	 * ArrayList.
	 * 
	 * @param tokens  The tokens that make up the vertex normal line. 
	 */ 
	private void processVertexNormal(String[] tokens) {
		if (tokens.length != 4) {
			System.out.println("Normal without 4 tokens!");
			for (String str:tokens) {
				System.out.println("Token :"+str);
			}
		}
		normals.add(Float.parseFloat(tokens[1]));
		normals.add(Float.parseFloat(tokens[2]));
		normals.add(Float.parseFloat(tokens[3]));
	}

	/**
	 * Routine to parse and add texture coordinate data 
	 * to the textures ArrayList.  This is not currently
	 * used or tested.
	 * 
	 * @param tokens  the tokens that make up the texture coordinate line. 
	 */
	private void processTextureCoordinate(String[] tokens) {
		texture.add(Float.parseFloat(tokens[1]));
		texture.add(Float.parseFloat(tokens[2]));
	}
    /**
     * Routine to parse information about smoothing.  
     * Currently not used -- ignores its input. 
     * @param tokens  the tokens that make up a smoothing line.
     */
	private void processSmoothing(String[] tokens) {
		// do nothing right now.
	}

	/**
	 * Routine to process a face.  Currently this is very limited. 
	 * 
	 * This is assuming it has vertices with normals. It will NOT process texture
	 * coordinates as written. I will change it (eventually) to handle vertices
	 * without normals or textures.
	 * 
	 * @param tokens  the tokens that make up the face description line. 
	 * 	 
	 */
	private void processFace(String[] tokens) {
		if (tokens.length != 10) {
			System.out.println("Unexpected number of tokens!");
			for (String str : tokens ) {
				System.out.println("token: "+str);
			}
		}
		triangles.add(Integer.parseInt(tokens[1]));
		triangles.add(Integer.parseInt(tokens[4]));
		triangles.add(Integer.parseInt(tokens[7]));
		triangles.add(Integer.parseInt(tokens[3]));
		triangles.add(Integer.parseInt(tokens[6]));
		triangles.add(Integer.parseInt(tokens[9]));
	}

	private void processGroup(String[] tokens) {
		// do nothing right now
	}

	/**
	 * Read the OBJ file -- This looks at the first token in the
	 * line and determines the type of directive.  It then hands 
	 * off all the tokens in the line to the appropriate "process"
	 * method.  
	 * 
	 * @param filename  The filename for the file to be read.  
	 */
	public void readOBJFile(String filename) {
		try {
			Scanner scanner = new Scanner(new File(filename));

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] tokens = line.split("[ /]");
				if (tokens.length > 0) { // handle blank line.
					if (tokens[0].equals("#")) {
						// do nothing -- it is a comment.
					} else if (tokens[0].equals("v")) {
						processVertex(tokens);
					} else if (tokens[0].equals("vn")) {
						processVertexNormal(tokens);
					} else if (tokens[0].equals("vt")) {
						processTextureCoordinate(tokens);
					} else if (tokens[0].equals("f")) {
						processFace(tokens);
					} else if (tokens[0].equals("s")) {
						processSmoothing(tokens);
					} else if (tokens[0].equals("g")) {
						processGroup(tokens);
					} else {
						// Unknown -- skip it for now.
					}
				}
			}
			/*
			 * Finished reading file.
			 */
			System.out.println(vertices.size()/3 + " vertices");
			System.out.println(normals.size()/3 + " normals");
			System.out.println(texture.size() + " textures");
			System.out.println(triangles.size()/6 + " faces");
			/*
			 * Dump Triangles and normals.
			 */
			int last = 0, lastNormal = 0;
			triangleList = new float[triangles.size()/6 *3 *4];
			normalList = new float [triangles.size() / 6 *3 * 3];
			for (int current = 0; current < triangles.size(); current += 6) {
				for (int coord = 0; coord < 3; coord++) {
					triangleList[last++] = vertices.get((triangles.get(current+coord) - 1)*3);
					triangleList[last++] = vertices.get((triangles.get(current+coord) - 1)*3+1);
					triangleList[last++] = vertices.get((triangles.get(current+coord) - 1)*3+2);
					triangleList[last++] = 1.0f;
					normalList[lastNormal++] = normals.get((triangles.get(current+coord+3) - 1)*3);
					normalList[lastNormal++] = normals.get((triangles.get(current+coord+3) - 1)*3 + 1);
					normalList[lastNormal++] = normals.get((triangles.get(current+coord+3) - 1)*3 + 2);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * default constructor.  Sets up original ArrayLists. 
	 */
	public OBJinfo() {
		vertices = new ArrayList<Float>(1000);
		normals = new ArrayList<Float>(1000);
		texture = new ArrayList<Float>(1000);
		triangles = new ArrayList<Integer>(1000);
	}
	
	/**
	 * Get the vertices from the model.  Vertices are 
	 * xyzw groups even though the wavefront OBJ files
	 * use only xyz groups.
	 * 
	 * @return  the array of vertices for this OBJ.
	 */
	public float[] getVertexList() {
		return triangleList;
	}
	/**
	 * Get the normals from the model.  
	 * 
	 * @return  the array of normals for this OBJ.
	 */	
	public float[] getNormalList() {
		return normalList;
	}

	/**
	 * An obsolete test stub...
	 * @param arguments  Command Line Arguments.
	 */
	public static void main(String[] arguments) {
		OBJinfo obj = new OBJinfo();
		obj.readOBJFile("src/cow.obj");
		System.exit(0);
	}

}
