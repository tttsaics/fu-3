
# 1. 負責打包的環境                                                                                              
    FROM maven:3.9-eclipse-temurin-11 AS builder                                                                     
    WORKDIR /app                                                                                                     
    COPY pom.xml .                                                                                                   
    COPY src ./src                                                                                                   
    RUN mvn clean package -DskipTests                                                                                
                                                                                                                     
    # 2. 負責執行的環境 (使用 Tomcat)                                                                                
    FROM tomcat:10.1-jdk11                                                                                           
    COPY --from=builder /app/target/*.war /usr/local/tomcat/webapps/ROOT.war                                         
    EXPOSE 8080                                                                                                      
    CMD ["catalina.sh", "run"]                                                                                       
                                  
