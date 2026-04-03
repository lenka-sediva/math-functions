#version 400
#define COLORED
in vec3 inPosition; // input from the vertex buffer
out vec3 color; //output color
uniform mat4 matMVP; // variable constant for all vertices in a single draw
uniform int time; // variable constant of time

const float PI = 3.14159;
const float scale = 2.0;

subroutine float shapeFunction(in vec2 pos);

// option 1
subroutine (shapeFunction ) float explicitFunction1(in vec2 pos){
	float offset = time/500.0;
	return 0.2*sin(scale*PI*pos.x + offset);
}

// option 2
subroutine (shapeFunction ) float explicitFunction2(in vec2 pos){
	float offset = time/500.0;
	return 0.2*sin(scale*PI*pos.y + offset);
}

subroutine uniform shapeFunction myShapeSelection;

void main() {
	// inPosition in range [0;1] on both axes is recalculated to range [-1;1]
	vec2 position = 2.0 * (inPosition.xy - vec2(0.5));
	
	// result of function calculation z = f(x,y)
	float resultZ = myShapeSelection(position.xy);
	
	//color calculation
	color = vec3(position.xy, resultZ);
	
	// transformation Model-View-Projection application
	gl_Position = matMVP * vec4(inPosition.xy, resultZ, 1.0);
} 
