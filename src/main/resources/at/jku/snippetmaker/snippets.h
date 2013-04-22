/*
 * snippets.h
 *
 *  Created on: 14.02.2011
 *      Author: sam
 */

#ifndef SNIPPETS_H_
#define SNIPPETS_H_

/**
 * utility constant, which can be set before including the snippet.h to indicate which step is the actual one
 * so the developer can test different version, which just one constant change
 */
#ifndef SNIPPET_STEP
# define SNIPPET_STEP 1000
#endif

/**
 * makro defining a insert snippet, with the given step description and optional a list of options
 * pattern:
 * <code>
 * #if SNIPPET_INSERT(...)
 *  code that should be inserted
 * #endif
 * </code>
 */
#define SNIPPET_INSERT(step, substep, description, ...) SNIPPET_STEP >= step
/**
 * makro defining a remove snippet, with the given step description and optional a list of options
 * pattern:
 * <code>
 * #if SNIPPET_REMOVE(...)
 *  code that should be removed at the desired step
 * #endif
 * </code>
 */
#define SNIPPET_REMOVE(step, substep, description, ...) SNIPPET_STEP < step
/**
 * makro defining an update snippet, with the given step description and optional a list of options
 * pattern:
 * <code>
 * #if SNIPPET_FROM_TO(...)
 *  code before the step
 * #else
 *  code after the step
 * #endif
 * </code>
 */
#define SNIPPET_FROM_TO(step, substep, description, ...) SNIPPET_STEP < step

#endif /* SNIPPETS_H_ */
