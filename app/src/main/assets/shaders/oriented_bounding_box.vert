#version 300 es

uniform mat4 u_ModelView;
uniform mat4 u_ModelViewProjection;
uniform vec3 a_Color;

//layout (location = 0) in vec4 a_Position;
//layout (location = 1) in vec3 a_Color;

out vec3 v_ViewPosition;
out vec3 v_ViewNormal;
out vec3 ourColor;

void main()
{
    v_ViewPosition = (u_ModelView * a_Position).xyz;
	gl_Position = u_ModelViewProjection * a_Position;
	ourColor = a_Color;
}