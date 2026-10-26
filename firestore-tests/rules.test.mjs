// Security-rules tests for the Medical Vault and profile, run against the local Firestore emulator.
//   npm install && npm test
import { test, before, after, beforeEach } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  initializeTestEnvironment, assertSucceeds, assertFails,
} from '@firebase/rules-unit-testing';
import {
  doc, getDoc, setDoc, updateDoc, deleteDoc, getDocs, collection, serverTimestamp, Bytes, writeBatch,
} from 'firebase/firestore';

const CHUNK = 921600;
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-medassist',
    firestore: { rules: readFileSync('../firestore.rules', 'utf8') },
  });
});
after(async () => { await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

const alice = () => env.authenticatedContext('alice', { email_verified: true }).firestore();
const bob = () => env.authenticatedContext('bob', { email_verified: true }).firestore();
const anon = () => env.unauthenticatedContext().firestore();

const bytes = (n) => Bytes.fromUint8Array(new Uint8Array(n).fill(7));
const sha = 'a'.repeat(64);

function chunk(db, uid, id, index, size = 10) {
  return setDoc(doc(db, `users/${uid}/documentChunks/${id}_${index}`), { documentId: id, index, data: bytes(size) });
}

function meta(uid, overrides = {}) {
  return {
    ownerId: uid, name: 'Blood test', category: 'blood_tests', notes: '', sizeBytes: 10,
    mimeType: 'application/pdf', chunkCount: 1, sha256: sha, storagePath: `users/${uid}/documentChunks`,
    uploadedAt: serverTimestamp(), ...overrides,
  };
}

async function uploadAsAlice(id = 'doc1') {
  const db = alice();
  await assertSucceeds(chunk(db, 'alice', id, 0));
  await assertSucceeds(setDoc(doc(db, `users/alice/documents/${id}`), meta('alice')));
  return db;
}

test('owner can upload pieces then metadata, and read both', async () => {
  const db = await uploadAsAlice();
  await assertSucceeds(getDoc(doc(db, 'users/alice/documents/doc1')));
  await assertSucceeds(getDoc(doc(db, 'users/alice/documentChunks/doc1_0')));
  await assertSucceeds(getDocs(collection(db, 'users/alice/documents')));
});

test('metadata is refused until its pieces exist', async () => {
  await assertFails(setDoc(doc(alice(), 'users/alice/documents/doc1'), meta('alice')));
  // Two pieces declared, only the first written.
  const db = alice();
  await assertSucceeds(chunk(db, 'alice', 'doc2', 0, CHUNK));
  await assertFails(setDoc(doc(db, 'users/alice/documents/doc2'), meta('alice', { sizeBytes: CHUNK + 5, chunkCount: 2 })));
  await assertSucceeds(chunk(db, 'alice', 'doc2', 1, 5));
  await assertSucceeds(setDoc(doc(db, 'users/alice/documents/doc2'), meta('alice', { sizeBytes: CHUNK + 5, chunkCount: 2 })));
});

test('another user cannot read, change or delete my documents', async () => {
  await uploadAsAlice();
  const b = bob();
  await assertFails(getDoc(doc(b, 'users/alice/documents/doc1')));
  await assertFails(getDoc(doc(b, 'users/alice/documentChunks/doc1_0')));
  await assertFails(getDocs(collection(b, 'users/alice/documents')));
  await assertFails(updateDoc(doc(b, 'users/alice/documents/doc1'), { name: 'hacked' }));
  await assertFails(deleteDoc(doc(b, 'users/alice/documents/doc1')));
  await assertFails(deleteDoc(doc(b, 'users/alice/documentChunks/doc1_0')));
  await assertFails(chunk(b, 'alice', 'evil', 0));
});

test('signed-out users get nothing', async () => {
  await uploadAsAlice();
  await assertFails(getDoc(doc(anon(), 'users/alice/documents/doc1')));
  await assertFails(chunk(anon(), 'alice', 'x', 0));
});

test('cannot store a document in someone else\'s folder or with a wrong owner', async () => {
  const b = bob();
  await assertSucceeds(chunk(b, 'bob', 'd', 0));
  await assertFails(setDoc(doc(b, 'users/bob/documents/d'), meta('bob', { ownerId: 'alice' })));
});

test('metadata validation: size limit, type, category, names, piece count', async () => {
  const db = alice();
  await assertSucceeds(chunk(db, 'alice', 'v', 0));
  const ref = doc(db, 'users/alice/documents/v');
  await assertFails(setDoc(ref, meta('alice', { sizeBytes: 10485761, chunkCount: 12 })));
  await assertFails(setDoc(ref, meta('alice', { mimeType: 'image/png' })));
  await assertFails(setDoc(ref, meta('alice', { category: 'secret' })));
  await assertFails(setDoc(ref, meta('alice', { name: '' })));
  await assertFails(setDoc(ref, meta('alice', { name: 'x'.repeat(101) })));
  await assertFails(setDoc(ref, meta('alice', { notes: 'x'.repeat(501) })));
  await assertFails(setDoc(ref, meta('alice', { chunkCount: 2 })));
  await assertFails(setDoc(ref, meta('alice', { sha256: 'nothex' })));
  await assertFails(setDoc(ref, { ...meta('alice'), base64: 'AAAA' }));
  await assertFails(setDoc(ref, meta('alice', { uploadedAt: new Date(0) })));
  await assertSucceeds(setDoc(ref, meta('alice')));
});

test('piece validation: binary only, size limit, id must match', async () => {
  const db = alice();
  const ref = doc(db, 'users/alice/documentChunks/p_0');
  await assertFails(setDoc(ref, { documentId: 'p', index: 0, data: 'QUFBQQ==' }));
  await assertFails(setDoc(ref, { documentId: 'p', index: 0, data: bytes(CHUNK + 1) }));
  await assertFails(setDoc(ref, { documentId: 'p', index: 1, data: bytes(5) }));
  await assertFails(setDoc(doc(db, 'users/alice/documentChunks/p_12'), { documentId: 'p', index: 12, data: bytes(5) }));
  await assertSucceeds(setDoc(ref, { documentId: 'p', index: 0, data: bytes(CHUNK) }));
  // Retry of an unfinished upload may overwrite a piece.
  await assertSucceeds(setDoc(ref, { documentId: 'p', index: 0, data: bytes(5) }));
});

test('a finished file is sealed: its pieces cannot be replaced', async () => {
  const db = await uploadAsAlice();
  await assertFails(chunk(db, 'alice', 'doc1', 0, 20));
});

test('owner can rename, recategorize and edit notes, but nothing else', async () => {
  const db = await uploadAsAlice();
  const ref = doc(db, 'users/alice/documents/doc1');
  await assertSucceeds(updateDoc(ref, { name: 'Renamed', category: 'prescriptions', notes: 'from Dr. Rao' }));
  await assertFails(updateDoc(ref, { sizeBytes: 1 }));
  await assertFails(updateDoc(ref, { ownerId: 'bob' }));
  await assertFails(updateDoc(ref, { category: 'nope' }));
  await assertFails(updateDoc(ref, { name: '' }));
});

test('owner can delete a document and its pieces in one batch', async () => {
  const db = await uploadAsAlice();
  const batch = writeBatch(db);
  batch.delete(doc(db, 'users/alice/documents/doc1'));
  batch.delete(doc(db, 'users/alice/documentChunks/doc1_0'));
  await assertSucceeds(batch.commit());
});

test('profile rules from Phase 2 still hold; other collections are closed', async () => {
  const profile = {
    displayName: 'Alice', email: 'a@example.com', preferredLanguage: 'en',
    narrationEnabled: false, dailyStepGoal: 8000, createdAt: serverTimestamp(),
  };
  await assertSucceeds(setDoc(doc(alice(), 'users/alice'), profile));
  await assertFails(getDoc(doc(bob(), 'users/alice')));
  await assertFails(setDoc(doc(alice(), 'users/alice/somethingElse/x'), { a: 1 }));
  await assertSucceeds(deleteDoc(doc(alice(), 'users/alice')));
});
