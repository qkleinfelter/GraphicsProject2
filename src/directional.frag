#version 330 core
// passthrough Fragment shader 
// Last update October 30, 2020
// 
// This version is used for testing and debugging
// it simply gets the color passed through from the 
// vertex shader.  No lighting calculations are done.
//
uniform vec3 ambientLight;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 halfVector;
uniform float shininess;
uniform float strength;

in vec4 Color;
in vec3 Normal;

out vec4 FragColor;
void main()
{
	float diffuse = max(0.0, dot(Normal, lightDirection));
	float specular = max(0.0, dot(Normal, halfVector));
	if (diffuse == 0.0) {
		specular = 0.0;
	} else {
		specular = pow(specular, shininess);
	}
	vec3 scatteredLight = ambientLight + lightColor * diffuse;
	vec3 reflectedLight = lightColor * specular * strength;
	vec3 rgb = min(Color.rgb * scatteredLight + reflectedLight, vec3(1.0));
    FragColor = vec4(rgb, Color.a);
//    if (diffuse == 0.0) {
//    	FragColor = vec4(1.0, 0.0, 0.0, 1.0);
//    }
//	FragColor = vec4(Color.rgb * ambientLight, Color.a);
	
}