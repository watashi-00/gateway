# Issues Found During Testing

## API

- Review all public classes and interfaces.

- Public classes and interfaces contain poorly named abstractions.

  - Add proper descriptions to all ports.
  - Rename generic public APIs. Suffixes such as `*Factory` and `*Builder` are not descriptive enough for new contributors. Prefer names that clearly express the component's responsibility.

- Public interfaces should not expose generic argument names.

  - e.g:

    - `int arg1` -> `int port`

## Bugs

### Ping Scheduler

- The ping scheduler receives telemetry correctly, but the node status displayed in the TUI is not updated.
- Node states such as `ONLINE`, `OFFLINE`, and `UNSTABLE` remain unchanged even after new telemetry is received.

### `requireToken()`

- `requireToken()` currently expects two arguments, even when authentication is disabled.
- The second argument should be optional when the first argument is `false`.

e.g:

```java
requireToken(false, null);
```

Should become:

```java
requireToken(false);
```


## Cluster 

- review flux of creation of a new cluster.

- on create the cluster automatically created on default gateway, but this is wrong 
![alt text](image-1.png)