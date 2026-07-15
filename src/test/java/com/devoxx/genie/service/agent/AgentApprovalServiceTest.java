package com.devoxx.genie.service.agent;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for the approval-dialog gating logic in {@link AgentApprovalService}.
 *
 * Issue #1209 regression: with write approvals disabled (auto-approve), a
 * blacklisted command must still force the approval dialog.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentApprovalServiceTest {

    @Mock
    private DevoxxGenieStateService stateService;

    @Test
    void requiresDialog_writeApprovalOn_noBlacklistMatch_showsDialog() {
        when(stateService.getAgentWriteApprovalRequired()).thenReturn(true);

        assertThat(AgentApprovalService.requiresDialog(stateService, null)).isTrue();
    }

    @Test
    void requiresDialog_writeApprovalOff_noBlacklistMatch_autoApproves() {
        when(stateService.getAgentWriteApprovalRequired()).thenReturn(false);

        assertThat(AgentApprovalService.requiresDialog(stateService, null)).isFalse();
    }

    @Test
    void requiresDialog_writeApprovalOff_blacklistMatch_stillShowsDialog() {
        // The core of issue #1209: auto-approve must NOT bypass the blacklist gate
        when(stateService.getAgentWriteApprovalRequired()).thenReturn(false);

        assertThat(AgentApprovalService.requiresDialog(stateService, "git reset --hard")).isTrue();
    }

    @Test
    void requiresDialog_writeApprovalOn_blacklistMatch_showsDialog() {
        when(stateService.getAgentWriteApprovalRequired()).thenReturn(true);

        assertThat(AgentApprovalService.requiresDialog(stateService, "git reset --hard")).isTrue();
    }
}
