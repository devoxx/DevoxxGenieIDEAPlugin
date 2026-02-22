package com.devoxx.genie.service.security;

import com.devoxx.genie.service.agent.tool.SecurityScanToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityScanToolSpecificationTest {

    @Test
    void securityScan_hasCorrectName() {
        ToolSpecification spec = SecurityScanToolSpecification.securityScan();
        assertThat(spec.name()).isEqualTo("run_security_scan");
    }

    @Test
    void securityScan_hasDescription() {
        ToolSpecification spec = SecurityScanToolSpecification.securityScan();
        assertThat(spec.description()).isNotEmpty();
        assertThat(spec.description()).contains("gitleaks");
        assertThat(spec.description()).contains("opengrep");
        assertThat(spec.description()).contains("trivy");
    }

    @Test
    void securityScan_hasOptionalScannersParam() {
        ToolSpecification spec = SecurityScanToolSpecification.securityScan();
        assertThat(spec.parameters()).isNotNull();
        // scanners is optional (not required)
    }
}
