package express;

import com.sun.net.httpserver.HttpExchange;
import express.http.Request;
import express.http.Response;

import java.util.ArrayList;
import java.util.HashMap;

public class ExpressContextThread implements Runnable {

  private final Express EXPRESS;
  private final HttpExchange HTTP_EXCHANGE;
  private final Request REQUEST;
  private final Response RESPONSE;

  private String requestPath;
  private String requestMethod;

  public ExpressContextThread(HttpExchange httpExchange, Express express) {
    this.EXPRESS = express;
    this.HTTP_EXCHANGE = httpExchange;
    this.REQUEST = new Request(httpExchange);
    this.RESPONSE = new Response(httpExchange);

    this.requestPath = REQUEST.getURI().getRawPath();
    this.requestMethod = REQUEST.getMethod();
  }

  @Override
  public void run() {
    ArrayList<ExpressHandler> middlewareAll = EXPRESS.MITTLEWARE.get("*");
    ArrayList<ExpressHandler> requestsAll = EXPRESS.REQUEST.get("*");
    ArrayList<ExpressHandler> middleware = EXPRESS.MITTLEWARE.get(requestMethod);
    ArrayList<ExpressHandler> requests = EXPRESS.REQUEST.get(requestMethod);

    if (middleware == null && middlewareAll != null)
      middleware = middlewareAll;
    else if (middlewareAll != null)
      middleware.addAll(middlewareAll);

    if (requests == null && requestsAll != null)
      requests = requestsAll;
    else if (requestsAll != null)
      requests.addAll(requestsAll);

    if (middleware != null && middleware.size() > 0) {
      middleware.forEach(exh -> {

        HashMap<String, String> params = exh.parseParams(requestPath);
        if (params != null || exh.getContext().equals("*")) {
          exh.getRequest().handle(REQUEST, RESPONSE);

          if (RESPONSE.isClosed())
            return;
        }
      });
    }

    if (requests != null && requests.size() > 0) {
      requests.forEach(exh -> {

        HashMap<String, String> params = exh.parseParams(requestPath);
        if (params != null || exh.getContext().equals("*")) {
          REQUEST.setParams(params);
          exh.getRequest().handle(REQUEST, RESPONSE);

          if (RESPONSE.isClosed())
            return;
        }
      });
    }
  }
}
