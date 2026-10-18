package com.suzhong.njl.wechat;

import java.net.URI;

public interface WechatWire {

    WechatHttpResult exchange(String method, URI uri, String body);
}
