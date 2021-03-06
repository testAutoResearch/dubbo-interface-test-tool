package com.whb.dubbo.service.impl;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.whb.dubbo.cache.CuratorCaches;
import com.whb.dubbo.cache.MethodCaches;
import com.whb.dubbo.cache.UrlCaches;
import com.whb.dubbo.client.DubboClient;
import com.whb.dubbo.context.ResponseDispatcher;
import com.whb.dubbo.dto.ConnectDTO;
import com.whb.dubbo.dto.ResultDTO;
import com.whb.dubbo.dto.UrlModelDTO;
import com.whb.dubbo.exception.RRException;
import com.whb.dubbo.handler.CuratorHandler;
import com.whb.dubbo.model.MethodModel;
import com.whb.dubbo.model.ServiceModel;
import com.whb.dubbo.model.UrlModel;
import com.whb.dubbo.service.ConnectService;
import com.whb.dubbo.util.ParamUtil;
import com.whb.dubbo.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service("connectService")
@Slf4j
public class ConnectServiceImpl implements ConnectService {

    @Override
    public ResultDTO<String> send(@NotNull ConnectDTO dto) throws Exception {

        log.info("begin to send {} .", JSON.toJSONString(dto));

        // get provider url
        URL url = UrlCaches.get(dto.getProviderKey()).getUrl();
        // get method
        MethodModel methodModel = MethodCaches.get(dto.getMethodKey());
        // parse parameter
        Object[] params = ParamUtil.parseJson(dto.getJson(), methodModel.getMethod());

        // 非常重要，必须要设置编码器协议类型
        url = url.addParameter(Constants.CODEC_KEY, "dubbo");
        DubboClient client = new DubboClient(url);
        client.doConnect();

        // set the path variables
        Map<String, String> map = ParamUtil.getAttachmentFromUrl(url);

        // create request.
        Request req = new Request();
        req.setVersion("2.0.0");
        req.setTwoWay(true);
        req.setData(new RpcInvocation(methodModel.getMethod(), params, map));

        client.send(req);

        // send timeout
        int timeout = (0 == dto.getTimeout()) ? 10 : dto.getTimeout();
        CompletableFuture<RpcResult> future = ResponseDispatcher.getDispatcher().getFuture(req);
        RpcResult result = future.get(timeout, TimeUnit.SECONDS);
        ResponseDispatcher.getDispatcher().removeFuture(req);

        return ResultDTO.createSuccessResult("SUCCESS",
                JSON.toJSONString(result.getValue(), SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat),
                String.class);
    }

    @Override
    public List<UrlModelDTO> listProviders(@NotNull ConnectDTO connect) throws NoSuchFieldException, IllegalAccessException {

        // get client
        CuratorHandler client = CuratorCaches.getHandler(connect.getConn());

        if (null == client) {
            throw new RRException("the cache is validate, please reconnect to zk againt.");
        }

        List<UrlModel> providers = client.getProviders(connect);

        // throw fast json error if you don't convert simple pojo
        // I have no idea why the UrlModel object will throw stack over flow exception.
        List<UrlModelDTO> ret = new ArrayList<>();
        providers.forEach(p -> {
            UrlModelDTO m = new UrlModelDTO();
            m.setKey(p.getKey());
            m.setHost(p.getUrl().getHost());
            m.setPort(p.getUrl().getPort());

            ret.add(m);
        });

        return ret;

    }

    /**
     * connect to zk and get all providers.
     *
     * @param conn
     * @return
     */
    @Override
    public List<ServiceModel> connect(@NotNull String conn) throws NoSuchFieldException, IllegalAccessException {

        // get client
        CuratorHandler client = CuratorCaches.getHandler(conn);

        if (!client.isAvailable()) {
            throw new RRException(StringUtil.format("can't connect to {}", conn));
        }

        // get providers
        List<ServiceModel> list = client.getInterfaces();


        return list;
    }
}
