package com.devbridge.backend.global.config;

/**
 * Mock meeting initializer was disabled.
 *
 * Reason:
 * - It previously used hard-coded workspace IDs such as "ws001" and "dummy-workspace-id".
 * - That caused meeting data and workspace memberships to be created under fixed mock workspaces.
 * - Runtime data must now be created only under the actual workspace selected by the user.
 *
 * If meeting seed data is needed later, create it through an explicit dev-only seed script
 * or API flow using a real workspace ID.
 */
public class MeetingDataInitializer {
}