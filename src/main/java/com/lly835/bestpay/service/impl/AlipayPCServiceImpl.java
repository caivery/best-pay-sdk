package com.lly835.bestpay.service.impl;

import com.lly835.bestpay.config.AliDirectPayConfig;
import com.lly835.bestpay.config.SignType;
import com.lly835.bestpay.constants.AlipayConstants;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.AbstractComponent;
import com.lly835.bestpay.service.BestPayService;
import com.lly835.bestpay.service.Signature;
import com.lly835.bestpay.service.impl.signature.AlipayPCSignatureImpl;
import com.lly835.bestpay.utils.JsonUtil;
import com.lly835.bestpay.utils.NameValuePairUtil;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 支付宝PC端支付(即时到账)
 * https://doc.open.alipay.com/docs/doc.htm?spm=a219a.7386797.0.0.0NDYyr&treeId=62&articleId=103566&docType=1
 * Created by null on 2017/2/14.
 */
class AlipayPCServiceImpl extends AbstractComponent implements BestPayService{

    private AliDirectPayConfig aliDirectPayConfig;

    public AlipayPCServiceImpl(AliDirectPayConfig aliDirectPayConfig) {
        Objects.requireNonNull(aliDirectPayConfig, "aliDirectPayConfig is null.");
        this.aliDirectPayConfig = aliDirectPayConfig;
    }

    public PayResponse pay(PayRequest request) throws Exception{

        logger.info("【支付宝PC端支付】request={}", JsonUtil.toJson(request));

        PayResponse response =  new PayResponse();
        response.setOrderId(request.getOrderId());
        response.setOrderAmount(request.getOrderAmount());

        //1. 封装参数
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("service", "create_direct_pay_by_user");
        requestMap.put("partner", this.aliDirectPayConfig.getPartnerId());
        requestMap.put("_input_charset", this.aliDirectPayConfig.getInputCharset());
        requestMap.put("out_trade_no", request.getOrderId());
        requestMap.put("subject", request.getOrderName());
        requestMap.put("payment_type", "1");
        requestMap.put("total_fee", String.valueOf(request.getOrderAmount()));
        requestMap.put("seller_id", this.aliDirectPayConfig.getPartnerId());
        requestMap.put("notify_url", this.aliDirectPayConfig.getNotifyUrl());
        requestMap.put("return_url", this.aliDirectPayConfig.getReturnUrl());

        //2. 签名
        Signature signature = new AlipayPCSignatureImpl(this.aliDirectPayConfig);
        String sign = signature.sign(requestMap);

        //这里特别注意, 即时到账和wap支付, sign_type参数不参与签名
        requestMap.put("sign_type", this.aliDirectPayConfig.getSignType().name());
        requestMap.put("sign", sign);
        logger.debug("【支付宝PC端支付】构造好的完整参数={}", JsonUtil.toJson(requestMap));

        //3. 构造url
        String url = new URIBuilder(AlipayConstants.ALIPAY_GATEWAY_NEW).setParameters(NameValuePairUtil.convert(requestMap)).toString();
        response.setRedirectUrl(url);

        logger.info("【支付宝PC端支付】respones={}", JsonUtil.toJson(response));
        logger.debug(url);

        return response;
    }

    @Override
    public PayResponse syncNotify(HttpServletRequest request) {

        //构造返回对象
        PayResponse response = new PayResponse();
        response.setOrderId(request.getParameter("out_trade_no"));
        response.setOrderId(request.getParameter("total_fee"));
        response.setTradeNo(request.getParameter("trade_no"));

        return response;
    }

    @Override
    public boolean verify(Map<String, String> toBeVerifiedParamMap, SignType signType, String sign) {
        return false;
    }

    @Override
    public PayResponse asyncNotify(HttpServletRequest request) {
        return null;
    }
}
