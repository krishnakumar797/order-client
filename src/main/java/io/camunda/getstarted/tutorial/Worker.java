package io.camunda.getstarted.tutorial;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.spring.client.annotation.ZeebeDeployment;
import io.camunda.zeebe.spring.client.annotation.ZeebeVariable;
import io.camunda.zeebe.spring.client.lifecycle.ZeebeClientLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableZeebeClient
@RestController
public class Worker {

  @Autowired private ZeebeClientLifecycle client;

  public static void main(String[] args) {
    SpringApplication.run(Worker.class, args);
  }

  @GetMapping("/status")
  public String getStatus() {
    Topology topology = client.newTopologyRequest().send().join();
    return topology.toString();
  }

  @GetMapping("/start/{value}")
  public String startWorkflowInstance(@PathVariable Integer value) {
    ProcessInstanceEvent workflowInstanceEvent = client
            .newCreateInstanceCommand()
            .bpmnProcessId("Process_d392d363-2c0f-4449-a387-f740afec3094")
            .latestVersion()
            .variables(Map.of("message_content", value))
            .send()
            .join();
    return workflowInstanceEvent.toString();
  }

  @ZeebeWorker(type = "order-create", autoComplete = true)
  public void orchestrateSomething(final ActivatedJob job) throws UnsupportedEncodingException {
    System.out.println(
        "Recieved data from the process variables: "
            + job.getVariables());
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("resultValue1", job.getVariablesAsMap().get("message_content"));
    client.newPublishMessageCommand()
            .messageName("order-receive")
            .correlationKey("")
            .variables(variables)
            .send().join();
  }


  @ZeebeWorker(type = "order-update", autoComplete = true)
  public void orderUpdate(final ActivatedJob job, @ZeebeVariable Integer resultValue1)  {
    System.out.println(
            "Result value in order-update: "
                    + resultValue1);
  }

  @ZeebeWorker(type = "order-hold", autoComplete = true)
  public void orderHold(final ActivatedJob job, @ZeebeVariable Integer resultValue1)  {
    System.out.println(
            "Result value in order-hold: "
                    + resultValue1);
  }

  @ZeebeWorker(type = "order-accepted", autoComplete = true)
  public void orderAccepted(final ActivatedJob job, @ZeebeVariable Integer resultValue1)  {
    System.out.println(
            "Order accepted message data from the process variables: "
                    + resultValue1);
  }
}
