#version 330 core
out vec4 FragColor;

in vec2 textureData;
in vec3 positionData;

uniform sampler2D TEXTURE_ATLAS;

void main()
{
    FragColor = texture(TEXTURE_ATLAS, textureData);
}