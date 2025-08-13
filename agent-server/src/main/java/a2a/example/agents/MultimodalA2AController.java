package a2a.example.agents;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.AgentCard;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("/multimodal")
public class MultimodalA2AController extends AbstractA2AController {

  @Resource(name = "multimodalJsonRpcHandler")
  private JSONRPCHandler multimodalJsonRpcHandler;

  @Override
  protected JSONRPCHandler getJSONRPCHandler() {
    return multimodalJsonRpcHandler;
  }

  @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Object handleRequest(@RequestBody String requestBody) {
    return super.handleRequest(requestBody);
  }

  @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
  public AgentCard getAgentCard() {
    return multimodalJsonRpcHandler.getAgentCard();
  }
}