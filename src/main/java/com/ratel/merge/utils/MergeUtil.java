package com.ratel.merge.utils;

import com.alibaba.fastjson.JSONObject;
import com.ratel.merge.annotation.MergeField;
import com.ratel.merge.consts.Consts;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @author zhangxn
 * @date 2021/12/27  23:33
 */
@Slf4j
public class MergeUtil {

    /***
     * 面向切面处理
     * @param pjp
     * @author zhangxn
     * @date 2021/12/29 23:30
     */
    public Object mergeData(ProceedingJoinPoint pjp) throws Throwable {
        Object proceed = pjp.proceed();
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method m = signature.getMethod();
            ParameterizedType parameterizedType = (ParameterizedType) m.getGenericReturnType();
            List<?> result = null;
            // 获取当前方法的返回值
            Type[] types = parameterizedType.getActualTypeArguments();
            Class clazz = ((Class) types[0]);
            result = (List<?>) proceed;
            mergeData(clazz, result);
            return result;
        } catch (Exception e) {
            log.error("某属性数据聚合失败", e);
            return proceed;
        }

    }

   /**
    * 数据聚合
    * @param clazz
    * @param result
    * @author zhangxn
    * @date 2021/12/29 23:29
    */
    public void mergeData(Class clazz, List<?> result) throws ExecutionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        List<Field> mergeFields = new ArrayList<Field>();
        List<Field> todoMergeFields = new ArrayList<Field>();
        List<Map<String, Object>> invokes = new ArrayList<>();
        Map<String, List<Field>> dataMapFields = new HashMap<>();
        Map<String, String> mergeName = new HashMap<>();
        // 获取属性
        for (Field field : fields) {
            MergeField annotation = field.getAnnotation(MergeField.class);
            Map<String, Object> map = new HashMap<>();
            if (annotation != null) {
                if (StringUtils.isEmpty(annotation.method())) {
                    todoMergeFields.add(field);
                    continue;
                }
                mergeFields.add(field);
                // 参数处理
                Set<String> params = new HashSet<>();
                result.forEach(obj -> {
                    field.setAccessible(true);
                    Object o = null;
                    try {
                        o = field.get(obj);
                        if (o != null) {
                            params.add(o.toString());
                        }
                    } catch (IllegalAccessException e) {
                        log.error("数据属性加工失败:" + field, e);
                        throw new RuntimeException("数据属性加工失败:" + field, e);
                    }
                });
                mergeName.put(field.getName(), annotation.mergeName());
                map.put(Consts.MERGE_NAME, annotation.mergeName());
                map.put(Consts.FEIGN, annotation.feign().getName());
                map.put(Consts.METHOD, annotation.method());
                map.put(Consts.VALUE, String.join(",", params));
                map.put(Consts.GROUP, annotation.group());
                map.put(Consts.NAME, field.getName());
                List<Field> fieldList = new ArrayList<>();
                if(dataMapFields.containsKey(annotation.group())) {
                    fieldList = dataMapFields.get(annotation.group());
                }
                fieldList.add(field);
                dataMapFields.put(annotation.group(), fieldList);
                invokes.add(map);
            }
        }
        // 同样方法的聚合
        Map<Object, List<Map<String, Object>>> groupMaps = invokes.stream().collect(Collectors.groupingBy(stringObjectMap -> stringObjectMap.get("group")));
        Map<String,  List<Map<String, Object>>> dataMap = new HashMap<>();
        for (Object object : groupMaps.keySet()) {
            List<Map<String, Object>> datas = groupMaps.get(object);
            List<Map<String, Object>> params = new ArrayList<>();
            datas.forEach(stringObjectMap -> {
                Map<String, Object> map = new HashMap<>();
                map.put(stringObjectMap.get("name").toString(), stringObjectMap.get("value").toString());
                params.add(map);
            });
            Object bean = SpringUtil.getBean((datas.get(0).get("feign").toString()));
            Method method = null;
            try {
                method = ClassLoader.getSystemClassLoader().loadClass(datas.get(0).get("feign").toString()).getMethod(datas.get(0).get("method").toString(), String.class);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            List<Map<String, Object>> values = (List<Map<String, Object>>) method.invoke(bean, JSONObject.toJSONString(params));
            // 为空就进行过滤
            if (values == null || values.isEmpty()) {
                continue;
            }
            dataMap.put(object.toString(), values);
        }
        // 查询数据为空直接返回
        if (dataMap.isEmpty()) {
            return;
        }
        // 数据聚合处理
        result.stream().forEach(obj -> {
            mergeFieldValue(obj, dataMapFields, todoMergeFields, dataMap, mergeName);
        });
    }

    /**
     * 单数据合并
     * @param clazz
     * @param mergeObj
     * @throws ExecutionException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public void mergeOne(Class clazz, Object mergeObj) throws ExecutionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        List<Field> mergeFields = new ArrayList<Field>();
        Map<String, Map<String, String>> invokes = new HashMap<>();
        // 获取属性
        for (Field field : fields) {
            MergeField annotation = field.getAnnotation(MergeField.class);
            if (annotation != null) {
                mergeFields.add(field);
                String args = "";
                field.setAccessible(true);
                Object o = null;
                try {
                    o = field.get(mergeObj);
                } catch (IllegalAccessException e) {
                    log.error("数据属性加工失败:" + field, e);
                    throw new RuntimeException("数据属性加工失败:" + field, e);
                }
                if (o != null) {
                    args = o.toString();
                }
                Object bean = SpringUtil.getBean(annotation.feign());
                Method method = annotation.feign().getMethod(annotation.method(), String.class);
                Map<String, String> value = (Map<String, String>) method.invoke(bean, args);
                invokes.put(field.getName(), value);
            }
        }
        mergeOneFieldValue(mergeObj, mergeFields, invokes);
    }

    /**
     * 合并对象属性值
     * @param mergeObj
     * @param mergeFields
     * @param toDomergeFields
     * @param dataMap
     * @param mergeNames
     */
    private void mergeFieldValue(Object mergeObj, Map<String, List<Field>> mergeFields, List<Field> toDomergeFields, Map<String, List<Map<String, Object>>> dataMap,Map<String, String> mergeNames) {
        for (String key : mergeFields.keySet()) {
            List<Map<String, Object>> mapList = dataMap.get(key);
            for (Field field : mergeFields.get(key)) {
                field.setAccessible(true);
                try {
                    Object o = field.get(mergeObj);
                    List<Map<String, Object>> map = mapList.stream().filter(stringObjectMap -> stringObjectMap.get(field.getName()).equals(String.valueOf(o))).collect(Collectors.toList());
                    List<Field> fields = toDomergeFields.stream().filter(field1 -> field1.getName().equals(mergeNames.get(field.getName()))).collect(Collectors.toList());
                    if (o != null && map != null && !map.isEmpty()) {
                        Field field1 =  fields.get(0);
                        field1.setAccessible(true);
                        field1.set(mergeObj, map.get(0).get(field1.getName()));
                    }
                } catch (IllegalAccessException e) {
                    log.error("数据属性加工失败:" + field, e);
                    throw new RuntimeException("数据属性加工失败:" + field, e);
                }
            }
        }
    }

    /**
     * 合并对象属性值
     * @param mergeObj
     * @param mergeFields
     * @param invokes
     */
    private void mergeOneFieldValue(Object mergeObj, List<Field> mergeFields, Map<String, Map<String, String>> invokes) {
        for (Field field : mergeFields) {
            field.setAccessible(true);
            Object o = null;
            try {
                o = field.get(mergeObj);
                if (o != null && invokes.get(field.getName()).containsKey(String.valueOf(o))) {
                    field.set(mergeObj, invokes.get(field.getName()).get(o.toString()));
                }
            } catch (IllegalAccessException e) {
                log.error("数据属性加工失败:" + field, e);
                throw new RuntimeException("数据属性加工失败:" + field, e);
            }
        }
    }
}
