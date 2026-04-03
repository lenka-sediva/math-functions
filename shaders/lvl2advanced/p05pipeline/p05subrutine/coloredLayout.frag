#version 400

in vec3 color; //input color 
out vec4 outColor; // output from the fragment shader

subroutine vec4 colorFunction ();

// set option 1
subroutine (colorFunction ) vec4 colorByColor() {
    return vec4(color, 1.0);
} 
 
// set option 2
subroutine (colorFunction ) vec4 colorByPossition() {
    return vec4(gl_FragCoord.xyz/100., 1.0);
}

#extension GL_ARB_explicit_uniform_location : enable
// set option 3 - define number ID of subroutine, see app code 
layout(index = 4) subroutine (colorFunction ) vec4 greyByPossition() {
    return vec4(gl_FragCoord.xxx/100., 1.0);
}

subroutine uniform colorFunction myColorSelection;

void main() {
	outColor = myColorSelection();
} 
