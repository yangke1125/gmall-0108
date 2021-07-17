package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "F:\\june\\rsa\\rsa.pub";
    private static final String priKeyPath = "F:\\june\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MjYzMTUyMjl9.EBB1VMtcMI2z4T7QkZxbTUzwEeyc-6nqb51mZJn2sCREiV90liPSiJEtBq_7BpW_pqbR2e9rtRKplF1WlRfduv5aV2CUJ11SpqfTtqykjlilsRa_8hKIeP7C0Xo8C5r8VxNsKe-p69H8W0j8h6HxjvYJ3Xl1FGebVaUnTq16tMdnZiy3a3JdOlvJ4TQLcPtoL1kTTeYGQDuxVLN5e9k4-u0GUC3DHv0ixDniZeOoeEJFKI3ipzyXw7Nt8FyRWvbOXYAkkXGGmLWdDzPOsvNZkQUIJnkREWZSD9fuVx4lLiuWNfYfQj5i8WZAr1dhd3cHH8S-L7aCSojmVYjsKZIv6A";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}