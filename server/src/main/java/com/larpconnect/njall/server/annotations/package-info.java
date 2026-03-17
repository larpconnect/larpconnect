/**
 * Contains strongly typed Guice qualifiers used to configure the LarpConnect HTTP server.
 *
 * <p>This package introduces custom annotations that serve as Guice qualifiers (e.g., binding
 * specific configuration settings to specific dependencies). By using dedicated qualifiers rather
 * than generic data types such as integers or strings, the server initialization process achieves
 * improved type safety and prevents ambiguous dependency injection.
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
package com.larpconnect.njall.server.annotations;

import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;
import javax.annotation.ParametersAreNonnullByDefault;
