package com.quangtd.qtcoin.domain;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Setter
@Getter
public class Response {
    private String data;

    @SuppressWarnings("unchecked")
    public static ResponseEntity createResponse(Object data, Class type) {
        return new ResponseEntity(new Gson().toJson(data, type), HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    public static ResponseEntity createResponse(Object data) {
        return new ResponseEntity(new Gson().toJson(data, String.class), HttpStatus.OK);
    }
}
