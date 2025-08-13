package a2a.example.agents;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.AgentCard;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("/flight")
public class FlightA2AController extends AbstractA2AController {

  @Resource(name = "flightJsonRpcHandler")
  private JSONRPCHandler flightJsonRpcHandler;

  @Override
  protected JSONRPCHandler getJSONRPCHandler() {
    return flightJsonRpcHandler;
  }

  @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Object handleRequest(@RequestBody String requestBody) {
    return super.handleRequest(requestBody);
  }

  @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
  public AgentCard getAgentCard() {
    return flightJsonRpcHandler.getAgentCard();
  }
}