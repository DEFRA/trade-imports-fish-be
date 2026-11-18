# Security Best Practices

This guide covers security considerations and best practices for the Trade Imports Fish Backend.

## Authentication & Authorization

### Bearer Token Authentication

API endpoints use Bearer token authentication:

```bash
curl -H "Authorization: Bearer <token>" \
  http://localhost:8085/example
```

**Configure OAuth2/OIDC**:

The application is configured for OAuth2/OIDC integration. Configure via environment:

```bash
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://your-idp.com
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=https://your-idp.com/.well-known/jwks.json
```

### Role-Based Access Control (RBAC)

Secure endpoints with roles:

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public Example create(@Valid @RequestBody Example entity) {
    return service.create(entity);
}
```

## Input Validation

### Bean Validation

Always validate input:

```java
public class Example {
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255)
    private String name;
    
    @NotBlank
    private String value;
}
```

### Database-Level Constraints

Add unique constraints:

```java
@Indexed(unique = true)
private String name;
```

### SQL Injection Prevention

Use parameterized queries (automatic with Spring Data):

```java
// Good: Spring Data handles parameterization
repository.findByName(name)

// Avoid: String concatenation
repository.findByName("'%' OR '1'='1'")
```

## Encryption

### TLS/HTTPS

Always use HTTPS in production:

```bash
export SERVER_SSL_ENABLED=true
export SERVER_SSL_KEY_STORE=/path/to/keystore.p12
export SERVER_SSL_KEY_STORE_PASSWORD=${KEYSTORE_PASSWORD}
```

### Database Encryption

Enable SSL for MongoDB connections:

```yaml
spring:
  data:
    mongodb:
      ssl:
        enabled: true
      uri: mongodb+srv://user:password@host/database
```

### Secrets Management

**Never commit secrets** to version control.

Use AWS Secrets Manager:

```java
@Value("${db.password}")
private String dbPassword;
```

Configure via environment:

```bash
export DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id db-password --query SecretString --output text)
```

## HTTP Security Headers

Configure security headers in `application.yml`:

```yaml
server:
  servlet:
    session:
      cookie:
        secure: true          # HTTPS only
        http-only: true       # No JS access
        same-site: strict     # CSRF protection
```

### Headers to Set

| Header | Value | Purpose |
|--------|-------|---------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPS enforcement |
| `X-Content-Type-Options` | `nosniff` | Prevent MIME sniffing |
| `X-Frame-Options` | `DENY` | Clickjacking protection |
| `Content-Security-Policy` | Restrict sources | XSS protection |

## CORS Configuration

Configure Cross-Origin Resource Sharing carefully:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("https://trusted-domain.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

## API Rate Limiting

Implement rate limiting to prevent abuse:

```java
@Configuration
public class RateLimitConfig {
    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.create(100);  // 100 requests/sec
    }
}
```

## Logging Security

### Avoid Logging Sensitive Data

```java
// Bad: Logs entire user object including passwords
log.info("User created: {}", user);

// Good: Only logs necessary information
log.info("User created with email: {}", user.getEmail());
log.debug("User role: {}", user.getRole());
```

### Sanitize Log Output

```java
// Bad: Logs sensitive headers
log.info("Headers: {}", request.getHeaders());

// Good: Filter sensitive headers
Map<String, String> safeHeaders = new HashMap<>();
request.getHeaders().forEach((k, v) -> {
    if (!k.equalsIgnoreCase("Authorization")) {
        safeHeaders.put(k, v);
    }
});
log.info("Headers: {}", safeHeaders);
```

## Dependency Security

### Keep Dependencies Updated

Regularly update dependencies:

```bash
# Check for vulnerabilities
mvn org.owasp:dependency-check-maven:check

# Update dependencies
mvn versions:update-properties
```

### Exclude Vulnerable Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>vulnerable-group</groupId>
            <artifactId>vulnerable-artifact</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

## Certificate Management

### Custom CA Certificates

Configure custom CA certificates for HTTPS connections:

```bash
export TRUSTSTORE_CDP_ROOT_CA=/path/to/ca.pem
```

The application automatically loads and trusts custom certificates via `CertificateLoader`.

## API Security Checklist

Before deploying to production, verify:

- [ ] HTTPS/TLS enabled
- [ ] Authentication/Authorization configured
- [ ] All inputs validated
- [ ] SQL injection protection via parameterized queries
- [ ] CORS configured appropriately
- [ ] Security headers set
- [ ] Secrets not in version control
- [ ] Error messages don't leak sensitive info
- [ ] Logging doesn't capture sensitive data
- [ ] Rate limiting configured
- [ ] Dependencies updated and vulnerable ones patched
- [ ] Database encryption enabled
- [ ] Session cookies secure/httpOnly/sameSite
- [ ] CSRF protection enabled

## Security Incident Response

### Detecting Security Issues

Monitor for:
- Unusual error rates
- Authentication failures
- Slow queries (possible injection attempts)
- Memory spikes (possible DoS)

### Response Plan

1. **Isolate**: Stop the affected service
2. **Analyze**: Review logs and metrics
3. **Patch**: Apply security fixes
4. **Test**: Verify fix in lower environment
5. **Deploy**: Roll out to production
6. **Monitor**: Watch for recurrence
7. **Document**: Record incident for review

## Compliance

### Common Compliance Requirements

- **GDPR**: Encrypt sensitive data, audit access
- **HIPAA**: Encryption, access controls, audit logs
- **PCI DSS**: Secure payment data, encryption, access control
- **SOC 2**: Logging, monitoring, incident response

Ensure your deployment meets requirements for your use case.

## See Also

- [Configuration](../configuration/index.md)
- [Authentication](authentication.md)
- [Architecture](../architecture/index.md)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
