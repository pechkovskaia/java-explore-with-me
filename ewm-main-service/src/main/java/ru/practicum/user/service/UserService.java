package ru.practicum.user.service;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto create(NewUserRequest dto);

    List<UserDto> getAll(List<Long> ids, int from, int size);

    void delete(Long userId);
}
