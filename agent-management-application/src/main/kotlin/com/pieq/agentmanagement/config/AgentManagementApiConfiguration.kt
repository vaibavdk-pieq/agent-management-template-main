package com.pieq.agentmanagement.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.pieq.core.config.PieqApplicationConfiguration

/**
 * Configuration class for the Pieq API application.
 *
 * Extends the base PieqApplicationConfiguration to include application-specific
 * configuration properties. This configuration is typically loaded from a YAML
 * configuration file (config_dev.yml).
 *
 * @property pieqConfig Application-specific configuration settings
 */
class AgentManagementApiConfiguration : PieqApplicationConfiguration() {
}

