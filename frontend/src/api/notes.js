import { apiClient } from "./client";

export function createNote({ title, content, type, coinIds, tags }) {
  return apiClient.post("/notes", { title, content, type, coinIds, tags });
}

export function getMyNotes() {
  return apiClient.get("/notes");
}
