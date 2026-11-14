package com.larpconnect.njall.data.domain.studios;

/** Classification type of a tenanted event entity. */
public enum EventType {
  /** Unspecified or legacy event classification. */
  EVENT_TYPE_UNSPECIFIED,
  /** Tabletop or live-action roleplaying game event (2nd-level CTI subtype). */
  GAME,
  /** Workshop event. */
  WORKSHOP,
  /** Conference event. */
  CONFERENCE,
  /** Community meetup event. */
  MEETUP
}
