package app.popdapplication.security;

import app.popdapplication.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@Getter
@AllArgsConstructor
public class UserData implements UserDetails {

    private UUID userId;
    private String username;
    private String password;
    private UserRole role;
    private boolean isAccountActive;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        SimpleGrantedAuthority role = new SimpleGrantedAuthority("ROLE_" + this.role.name());
        return List.of(role);
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.isAccountActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.isAccountActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.isAccountActive;
    }

    @Override
    public boolean isEnabled() {
        return this.isAccountActive;
    }
}
