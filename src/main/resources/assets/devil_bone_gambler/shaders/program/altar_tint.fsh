#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvProjMat;
uniform vec3 SphereCenterView;
uniform float Radius;
uniform float RedGain;
uniform float LumaGain;

in vec2 texCoord;

out vec4 fragColor;

void main()
{
    vec4 color = texture(DiffuseSampler, texCoord);

    /*
     * Rebuild the view space position from the depth buffer, so the sphere test is independent of
     * whatever the terrain renderer did. The depth buffer holds window depth, which is the clip
     * depth mapped to the unit interval.
     */
    float depth = texture(DepthSampler, texCoord).r;
    vec4 clip = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InvProjMat * clip;
    view /= max(abs(view.w), 0.00001);

    float distanceToCenter = length(view.xyz - SphereCenterView);
    float mask = 1.0 - smoothstep(Radius - 1.0, Radius, distanceToCenter);

    if (mask <= 0.0)
    {
        fragColor = color;
        return;
    }

    /*
     * Red is already clamped to the unit interval before this pass, so the tint attenuates green and
     * blue instead of raising red. The result reads as a darker red cast.
     */
    vec3 tinted = color.rgb * vec3(1.0, RedGain, RedGain) * LumaGain;
    fragColor = vec4(mix(color.rgb, tinted, mask), color.a);
}
