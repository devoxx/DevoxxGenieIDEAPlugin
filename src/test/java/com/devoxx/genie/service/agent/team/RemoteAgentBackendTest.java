package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.model.agent.AgentResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TASK-248: mapping of the DockerAgents orchestrator-api wait payload into AgentResult.
 */
class RemoteAgentBackendTest {

    @Test
    void parseWaitResponse_okResult() {
        String body = """
                {"session_id": "reviewer-ab12cd34", "status": "exited", "exit_code": 0,
                 "result": {"session_id": "reviewer-ab12cd34", "agent": "reviewer",
                            "status": "ok", "exit_code": 0,
                            "summary": "Looks good; one nit in Foo.java.",
                            "parent_session_id": null, "intent": "review foo"}}
                """;
        AgentResult result = RemoteAgentBackend.parseWaitResponse(body, "reviewer", "review foo", 1234);

        assertThat(result.status()).isEqualTo(AgentResult.Status.OK);
        assertThat(result.summary()).isEqualTo("Looks good; one nit in Foo.java.");
        assertThat(result.agent()).isEqualTo("reviewer");
        assertThat(result.provider()).isEqualTo("remote");
    }

    @Test
    void parseWaitResponse_errorResult() {
        String body = """
                {"result": {"agent": "implementer", "status": "error", "exit_code": 124,
                            "summary": "session exceeded max duration"}}
                """;
        AgentResult result = RemoteAgentBackend.parseWaitResponse(body, "implementer", null, 10);

        assertThat(result.status()).isEqualTo(AgentResult.Status.ERROR);
        assertThat(result.summary()).contains("exceeded max duration");
    }

    @Test
    void parseWaitResponse_missingSummaryAndGarbage_stayReadable() {
        AgentResult noSummary = RemoteAgentBackend.parseWaitResponse(
                "{\"session_id\": \"x-1\", \"result\": {\"status\": \"ok\", \"exit_code\": 0}}",
                "x", null, 5);
        assertThat(noSummary.summary()).contains("no summary");

        AgentResult garbage = RemoteAgentBackend.parseWaitResponse("not json", "x", null, 5);
        assertThat(garbage.status()).isEqualTo(AgentResult.Status.ERROR);
        assertThat(garbage.summary()).contains("Unreadable");
    }

    @Test
    void parseResultPayload_plainResultJsonWithCustomLabel() {
        // The LOCAL_CONTAINER runner reads result.json straight from the artifacts mount:
        // top-level fields, no wait envelope, "container" provider label.
        String body = """
                {"session_id": "reviewer-ab12cd34", "agent": "reviewer", "status": "ok",
                 "exit_code": 0, "summary": "All good.", "parent_session_id": null, "intent": null}
                """;
        AgentResult result = RemoteAgentBackend.parseResultPayload(body, "reviewer", null, 42, "container");

        assertThat(result.status()).isEqualTo(AgentResult.Status.OK);
        assertThat(result.summary()).isEqualTo("All good.");
        assertThat(result.provider()).isEqualTo("container");
    }

    @Test
    void parseWaitResponse_okStatusWithNonZeroExit_isError() {
        String body = "{\"result\": {\"status\": \"ok\", \"exit_code\": 1, \"summary\": \"partial\"}}";
        assertThat(RemoteAgentBackend.parseWaitResponse(body, "x", null, 5).status())
                .isEqualTo(AgentResult.Status.ERROR);
    }
}
