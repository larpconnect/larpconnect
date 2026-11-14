package com.larpconnect.njall.data.domain.admin;

/** Status of an administrative user account. */
public enum AdminUserStatus {
  /** In-memory/API sentinel for an unspecified status. */
  UNKNOWN,
  ACTIVE,
  DISABLED,
  DELETED
}
