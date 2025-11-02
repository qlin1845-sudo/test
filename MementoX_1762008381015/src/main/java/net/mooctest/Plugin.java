package net.mooctest;

public interface Plugin {
    String getName();
    void execute(UserManager userManager);
}
