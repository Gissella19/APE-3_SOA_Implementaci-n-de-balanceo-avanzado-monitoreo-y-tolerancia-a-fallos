package ec.edu.uta.venta_vehiculos_api.dto;

public class TokenResponse {

    private String access_token;
    private String token_type;
    private String refresh_token;
    private long expires_in;
    private String scope;

    public TokenResponse() {
    }

     public TokenResponse(String access_token,
            String token_type,
            String refresh_token,
            long expires_in,
            String scope) {
                this.access_token=access_token;
                this.token_type = token_type;
                this.refresh_token = refresh_token;
                this.expires_in = expires_in;
                this.scope = scope;
    }


    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public void setExpires_in(long expires_in) {
        this.expires_in = expires_in;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }


     public String getAccess_token() {
         return access_token;
     }

     public String getToken_type() {
         return token_type;
     }

     public String getRefresh_token() {
         return refresh_token;
     }

     public long getExpires_in() {
         return expires_in;
     }

     public String getScope() {
         return scope;
     }
    
}
