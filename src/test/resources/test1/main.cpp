#include <iostream>
#include <cstdlib>

#include <GLee.h>
#include <GL/gl.h>		// OpenGL header
#include <GL/glu.h>		// OpenGL Utility header
#include <GL/glut.h>	// GLUT header

#include <oogl/gl_error.h>
#include <oogl/timer.h>

#include <oogl/Model.h>

#include <snippets.h>
#define SNIPPET_STEP 9

oogl::Model* complexModel = NULL;
float animatedangle = 0;

void cleanup();

void init() {
	atexit(cleanup);

	complexModel = oogl::loadModel("models/NabooFighter.3ds", oogl::Model::LOAD_NORMALIZE_TWO);

	glEnable(GL_DEPTH_TEST);

#if SNIPPET_FROM_TO(5,1, "Light")
	/* 
	// Uncomment to enable lighting
	glEnable(GL_COLOR_MATERIAL);
	glEnable(GL_LIGHTING);
	glEnable(GL_LIGHT0);//*/
#else
	// Uncomment to enable lighting
	glEnable(GL_COLOR_MATERIAL);
	glEnable(GL_LIGHTING);
	glEnable(GL_LIGHT0);
#endif

	glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
}

void cleanup() {
	delete complexModel;
}

/**
 * called when a frame should be rendered
 */
void display() {
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

#if SNIPPET_INSERT(2,1, "insert a triangle")
# if SNIPPET_FROM_TO(3,1, "What about some colors")
	glBegin(GL_TRIANGLES);
		glVertex3f(-2,-1.5,-5.0);
		glVertex3f( 2,-1.5,-5.0);
		glVertex3f( 0, 1.5,-5.0);
	glEnd();
# else
#  if SNIPPET_FROM_TO(4,1, "a standard teapot instead of a triangle")
	glBegin(GL_TRIANGLES);
		glColor3f(1,0,0);
		glVertex3f(-2,-1.5,-5.0);

		glColor3f(0,1,0);
		glVertex3f( 2,-1.5,-5.0);

		glColor3f(0,0,1);
		glVertex3f( 0, 1.5,-5.0);
	glEnd();
#  else
#   if SNIPPET_INSERT(6,1, "I like to move it")
	glLoadIdentity();
#    if SNIPPET_FROM_TO(9,1, "add planet")
	glTranslatef(0,0,-5);
#    else
#     if SNIPPET_FROM_TO(10,1, "add sun")
	glTranslatef(0,0,-25);
#	  else
	glTranslatef(0,0,-80);
#	  endif
#    endif
#   endif

#   if SNIPPET_FROM_TO(7,1, "getting more complex model")
	glColor3f(0,1,0);
	glutSolidTeapot(1);
#   else
#    if SNIPPET_INSERT(10,2, "add sun")	
	glColor3f(1,1,0);
	glutSolidSphere(15,30,30);
	glRotatef(animatedangle/2,0,1,0);
	glTranslatef(30,0,0);
#	 endif
#    if SNIPPET_INSERT(9,2, "add planet")	
	glColor3f(0,0,1);
	glutSolidSphere(7,30,30);
#	 endif
#    if SNIPPET_INSERT(8,1, "I like to move it 2")
	glRotatef(animatedangle,0,1,0);
#    endif
#    if SNIPPET_INSERT(9,3, "add planet")
	glTranslatef(0,0,-10);
#	 endif
	complexModel->render();
#   endif
#  endif
# endif
#endif

	LOG_GL_ERRORS();
	glutSwapBuffers();
}

/**
 * called when the window was resized
 * @param w new window width in pixel
 * @param h new window height in pixel
 */
void reshape(int w, int h) {
	glViewport(0, 0, w, h);

	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	gluPerspective(45, ((float)w)/h,0.1f, 1000);

	glMatrixMode(GL_MODELVIEW);
	glLoadIdentity();
}

/**
 * called when nothing else needs to be done
 */
void idle() {
	//force a redisplay
	glutPostRedisplay();
}

/**
 * called when the user pressed a key
 * @param key ASCII character code
 * @param x mouse x position in pixel relative to the window, when the key was pressed
 * @param y mouse y position in pixel relative to the window, when the key was pressed
 */
void keyboard(unsigned char key, int x, int y) {
	switch (key) {
	case 27: //27=esc
		exit(0);
		break;
#if SNIPPET_INSERT(1,1,"changing background color")
	case 'b':
		std::cout << "change clear color to dark blue" << std::endl;
		glClearColor(0,0,0.5f,1.0f);
		break;
	case 'g':
		std::cout << "change clear color to dark green" << std::endl;
		glClearColor(0,0.5f,0,1.0f);
		break;
#endif
	}
	glutPostRedisplay();
}

/**
 * called when the user pressed or released a mouse key
 * @param button which mouse button was pressed, one of GLUT_LEFT_BUTTON, GLUT_MIDDLE_BUTTON and GLUT_RIGHT_BUTTON
 * @param state button pressed (GLUT_DOWN) or released(GLUT_UP)
 * @param x mouse x position in pixel relative to the window, when the mouse button was pressed
 * @param y mouse y position in pixel relative to the window, when the mouse button was pressed
 */
void mouse(int button, int state, int x, int y) {
	glutPostRedisplay();
}

/**
 * called when the mouse moves
 * @param x mouse x position in pixel relative to the window
 * @param y mouse x position in pixel relative to the window
 */
void mouseMotion(int x, int y) {
}

void update(int value) {
	animatedangle += 1;
	glutPostRedisplay();
	glutTimerFunc(25, update, 0); //call again after 25 ms
}

int setupGLUT(int argc, char** argv) {
	glutInit(&argc, argv);
	// glutInitContextVersion(3, 0);
	glutInitDisplayMode(GLUT_RGBA | GLUT_DOUBLE | GLUT_DEPTH);
	glutInitWindowSize(800, 600);
	glutInitWindowPosition(100, 100);

	int windowId = glutCreateWindow("ex1Introduction");

	glutDisplayFunc(display);
	glutIdleFunc(idle);
	glutReshapeFunc(reshape);
	glutKeyboardFunc(keyboard);
	glutMouseFunc(mouse);
	glutMotionFunc(mouseMotion);

	//update for animations
	glutTimerFunc(25, update, 0); //call after 25 ms

	return windowId;
}

int main(int argc, char** argv) {
	setupGLUT(argc, argv);

	oogl::dumpGLInfos();

	init();

	glutMainLoop();

	return 0;
}
