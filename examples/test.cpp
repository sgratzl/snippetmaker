#include <iostream>

#define SNIPPET_STEP 3
#include <snippets.h>

int main(int argc, char** argv) {
#if SNIPPET_FROM_TO(2,1,"manipulate out")
# if SNIPPET_INSERT(1,1,"std::cout",useComments)
	std::cout << "Hello World" << std::endl;
# endif
#else
	std::cout << "Hello World" << " to all of you" << std::endl;
#endif

#if SNIPPET_REMOVE(3,1,"remove test")
	//sample program
#endif
	return 0;
}
