package com.baize.patcher.api.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.lang.reflect.Type;

abstract public class JsonUtil {
	public static String encode(Object json) {
		return new Gson().toJson(json);
	}

	public static String encode(Object json, boolean isPretty) {
		return (isPretty ? new GsonBuilder().setPrettyPrinting().create() : new Gson()).toJson(json);
	}

	public static Object decode(String json) {
		return new JsonParser().parse(json);
	}

	public static <T> T decode(String json, Type typeOfT) {
		return new Gson().fromJson(json, typeOfT);
	}

	public static <T> T decode(String json, Class<T> classOfT) {
		return new Gson().fromJson(json, classOfT);
	}
}
