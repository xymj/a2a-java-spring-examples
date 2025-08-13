package a2a.example.agents;


import a2a.example.agents.AbstractA2AController;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.AgentCard;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/novel")
public class NovelA2AController extends AbstractA2AController {

  @Resource(name = "novelJsonRpcHandler")
  private JSONRPCHandler novelJsonRpcHandler;

  @Override
  protected JSONRPCHandler getJSONRPCHandler() {
    return novelJsonRpcHandler;
  }

  @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Object handleRequest(@RequestBody String requestBody) {
    return super.handleRequest(requestBody);
  }

  @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
  public AgentCard getAgentCard() {
    return novelJsonRpcHandler.getAgentCard();
  }
}