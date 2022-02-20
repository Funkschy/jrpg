
attribute vec4 a_position;
attribute vec2 a_texcoord;

uniform mat4 u_matrix;
uniform mat4 u_texture_matrix;

varying vec2 v_texcoord;

void main() {
        gl_Position = u_matrix * a_position;
        v_texcoord = (u_texture_matrix * vec4(a_texcoord, 0.0, 1.0)).xy;
}
