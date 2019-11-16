package com.my.blog.website.aspect;

import com.my.blog.website.annotations.SystemMapperLog;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 切面声明
 *
 * @author 文希
 * @create 2019-11-16 21:24
 */
@Component
@Aspect
public class LogAopActionAspect {

    /**
     * 切点定义
     */
    private final String MAPPER_POINT = "execution(public * com.my.blog.website.dao.*(..))";

    //mapper层切点
    @Pointcut(MAPPER_POINT)
    private void mapperAspect() {
    }

    //mapper层切入后的处理方法-环绕
    @SuppressWarnings("unchecked")
    @Around("mapperAspect()")
    public Object doAroundMapper(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Class[] parameterTypes = ((MethodSignature) pjp.getSignature()).getMethod().getParameterTypes();
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            //如果有注解，说明是需要监听的方法
            if(method.isAnnotationPresent(SystemMapperLog.class)){
                SysUser sysUser = UserUtils.getUser();
                Object[] args = pjp.getArgs();
                //从注解中获取需要的信息
                HashMap<String,String> map = getMapperMthodDescription(pjp);
                String operation = map.get("operation");
                String table = map.get("table");
                String columns = map.get("columns");
                String columnsName = map.get("columnsName");
                String param = map.get("param");
                String operateObject = map.get("operateObject");
                String type = map.get("type");
                String condition = map.get("condition");
                String[] columnNameArr = null;//获取操作行名称
                Map map1= new HashMap();//储存方法内参数
                List<ChangeList> changeList = new ArrayList();//储存新旧值变化
                if(args != null && args.length != 0 && !("deleteA").equals(type)){
                    map1 = BeanUtils.describe(args[0]);
                }
                if(("save").equals(type)){
                    String[] paramArr = null;
                    //如果是保存
                    //判断数据是否满足条件，不满足不需要保存日志
                    if(StringUtils.isNoneBlank(condition)){
                        //第一步拆分，拆分出每个条件
                        String[] conArr = condition.split(",");
                        for (int i = 0; i < conArr.length; i++) {
                            //第二部拆分，拆分出每个条件的key和value
                            String[] conditionArr = conArr[i].split("\\|");
                            if(!(conditionArr[1].equals((String)map1.get(conditionArr[0])))){
                                //如果不满足条件，直接跳出方法
                                Object result = pjp.proceed();
                                return result;
                            }
                        }
                    }
                    //如果有param从参数中取出
                    if(StringUtils.isNoneBlank(param)){
                        paramArr = param.split(",");
                        //如果填写了自定义拼接操作需要的信息，开始拼接操作信息
                    }
                    if(operation.indexOf("param") != -1){
                        String[] operationArr = operation.split("param");
                        operation = "";
                        for (int i = 0; i < operationArr.length; i++) {
                            //根据|分割
                            String[] pa = paramArr[i].split("\\|");
                            String str = "";
                            for (int j = 0; j < pa.length; j++) {
                                str = (String)map1.get(StringUtils.toCamelCase(pa[j]));
                                if(StringUtils.isBlank(str)){
                                    continue;
                                }else{
                                    break;
                                }
                            }
                            operation += operationArr[i] + str;
                        }
                    }
                }else if(("update").equals(type)){
                    if(StringUtils.isNoneBlank(table)){
                        columnNameArr = columnsName.split(",");
                    }
                    //根据所获取的信息拼接出日志对象
                    if(StringUtils.isNoneBlank(table)&&StringUtils.isNoneBlank(columns)){
                        String paramStr = "";
                        String[] paramArr = null;
                        //若传入的参数中存在delFlag且他的值为0，则认定此次操作为假删除，不处理各个字段的变化，保存一条删除记录
                        boolean isDel = false;//假删除标识
                        String[] operationArr = operation.split("param");
                        if(("0").equals((String)map1.get("delFlag"))){
                            isDel = true;
                            operationArr[0] = "删除记录";
                        }
                        if(StringUtils.isNoneBlank(param)){
                            paramStr = ","+param;
                            paramArr = param.split(",");
                        }
                        //如果填写了表信息和字段信息
                        String sql = "select " + columns + paramStr + " from " + table + " where id='"+map1.get("id")+"'";
                        if((columns.indexOf(",") != -1) && !isDel){
                            //如果列中存在逗号，说明是多列
                            String[] columnArr = columns.split(",");
                            for (int i = 0; i < columnArr.length; i++) {
                                ChangeList cl = new ChangeList();
                                String oldValue = DBUtils.getString(sql, columnArr[i]);
                                String newValue = (String)map1.get(StringUtils.toCamelCase(columnArr[i]));
                                try {
                                    //根据北京时间格式转换新值。有异常说明不是时间格式
                                    String DATE_FORMAT = "EEE MMM dd HH:mm:ss z yyyy";
                                    Date date = new SimpleDateFormat(DATE_FORMAT, Locale.US).parse(newValue);
                                    SimpleDateFormat format0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    newValue = format0.format(date);
                                    //如果新值是时间格式。将旧值的.0处理掉
                                    oldValue = oldValue.substring(0,oldValue.length()-2);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                                //如果要修改的新值为空。不予记录
                                if(StringUtils.isBlank(newValue)){
                                    continue;
                                }
                                cl.setColumn(columnArr[i]);
                                cl.setOldValue(oldValue);
                                cl.setNewValue(newValue);
                                changeList.add(cl);
                            }
                        }else if(columns.indexOf(",") == -1){
                            //单列
                            ChangeList cl = new ChangeList();
                            cl.setColumn(columns);
                            cl.setOldValue(DBUtils.getString(sql, columns));
                            cl.setNewValue((String)map1.get(StringUtils.toCamelCase(columns)));
                            changeList.add(cl);
                        }
                        //如果填写了自定义拼接操作需要的信息，开始拼接操作信息
                        if(operation.indexOf("param") != -1){
                            operation = "";
                            for (int i = 0; i < operationArr.length; i++) {
                                operation += operationArr[i] + DBUtils.getString(sql, paramArr[i]);
                            }
                        }
                    }
                }else if(type.indexOf("delete") != -1){
                    //如果是删除
                    String[] paramArr = null;
                    if(StringUtils.isNoneBlank(param)){
                        paramArr = param.split(",");
                    }
                    String sql = "";
                    if(("deleteA").equals(type)){
                        //如果delete的传入值是list
                        sql = "select " + param + " from " + table + " where id='"+args[0].toString()+"'";
                    }else{
                        //如果delete的穿入值是对象
                        sql = "select " + param + " from " + table + " where id='"+map1.get("id")+"'";
                    }
                    //如果填写了表信息和字段信息
                    if(operation.indexOf("param") != -1){
                        String[] operationArr = operation.split("param");
                        operation = "";
                        for (int i = 0; i < operationArr.length; i++) {
                            operation += operationArr[i] + DBUtils.getString(sql, paramArr[i]);
                        }
                    }
                }
                SystemLog systemLog = new SystemLog();
                systemLog.setUserAccount(sysUser.getUserName());
                systemLog.setDelFlag("1");
                systemLog.setType("9");
                systemLog.setOperationCode("1");
                systemLog.setCampId(sysUser.getCampId());
                systemLog.setGroupId(sysUser.getGroupId());
                systemLog.setOperateObject(operateObject);
                systemLog.setOperation(operation);
                if(changeList.size() != 0){
                    for (int i = 0; i < changeList.size(); i++) {
                        if(!changeList.get(i).getOldValue().equals(changeList.get(i).getNewValue())){
                            systemLog.setId(GeneratorUUID.getId());
                            systemLog.setOldValue(changeList.get(i).getOldValue());
                            systemLog.setNewValue(changeList.get(i).getNewValue());
                            //获取操作对象名
                            systemLog.setFeeItem(columnNameArr[i]);
                            systemLog.preInsert();
                            systemLogMapper.save(systemLog);
                        }
                    }
                }else{
                    systemLog.preInsert();
                    systemLogMapper.save(systemLog);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e.getMessage());
        }
        Object result = pjp.proceed();
        return result;
    }

    /**
     * 获取注解中对方法的描述信息 用于mapper层注解
     *
     * @param joinPoint
     *            切点
     * @return 方法描述
     * @throws Exception
     */
    public static HashMap getMapperMthodDescription(JoinPoint joinPoint)
            throws Exception {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = signature.getName();
        Object[] arguments = joinPoint.getArgs();
        HashMap<String,String> map = new HashMap<String,String>();
        if (method.getName().equals(methodName)) {
            Class[] clazzs = method.getParameterTypes();
            if (clazzs.length == arguments.length) {
                map.put("operation", method.getAnnotation(SystemMapperLog.class).operation());
                map.put("table", method.getAnnotation(SystemMapperLog.class).table());
                map.put("columns", method.getAnnotation(SystemMapperLog.class).columns());
                map.put("columnsName", method.getAnnotation(SystemMapperLog.class).columnsName());
                map.put("operateObject", method.getAnnotation(SystemMapperLog.class).operateObject());
                map.put("param", method.getAnnotation(SystemMapperLog.class).param());
                map.put("type", method.getAnnotation(SystemMapperLog.class).type());
                map.put("condition", method.getAnnotation(SystemMapperLog.class).condition());
            }
        }
        return map;
    }
}
