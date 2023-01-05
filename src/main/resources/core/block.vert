#version 330 core
layout (location = 0) in vec3 inputPosition;
layout (location = 1) in vec2 inputTexCoords;

out vec2 textureData;
out vec3 positionData;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main()
{
    gl_Position = projection * view * model * vec4(inputPosition, 1.0);
    textureData = inputTexCoords;
}