#version 330 core
//
// Pass through Vertex shader.  
// Passes vertex information through without changing it.
//  Being used for debugging purposes.
// 
uniform mat4 modelingMatrix;
uniform mat4 viewingMatrix;
uniform mat4 projectionMatrix;
uniform mat4 normalMatrix;

in vec4 vPosition;
in vec3 vNormal;

out vec4 Color;
out vec3 Normal;

void main()
{
	Color = vec4(0.8,0.8,0.8,1.0);
	Normal = mat3(normalMatrix) * vNormal;
    gl_Position = projectionMatrix * viewingMatrix * modelingMatrix * vPosition;
}