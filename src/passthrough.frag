#version 330 core
// passthrough Fragment shader 
// Last update October 30, 2020
// 
// This version is used for testing and debugging
// it simply gets the color passed through from the 
// vertex shader.  No lighting calculations are done.
//

in vec4 Color;

out vec4 FragColor;
void main()
{
		FragColor = vec4(1.0, 1.0, 1.0, 1.0);// Color;
}