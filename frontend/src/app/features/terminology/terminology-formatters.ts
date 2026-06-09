import { Citation, ContactInfo } from './terminology.models';

export function formatCitation(citation: Citation | null | undefined): string {
  if (!citation) {
    return 'n/a';
  }

  if (citation.unstructuredValue) {
    return citation.unstructuredValue;
  }

  return compactParts([
    citation.author,
    citation.address,
    citation.organization,
    citation.editor,
    citation.title,
    citation.contentDesignator,
    citation.mediumDesignator,
    citation.edition,
    citation.placeOfPublication,
    citation.publisher,
    citation.dateOfPublication,
    citation.dateOfRevision,
    citation.location,
    citation.extent,
    citation.series,
    citation.notes
  ]);
}

export function formatContact(contact: ContactInfo | null | undefined): string {
  if (!contact) {
    return 'n/a';
  }

  if (contact.value) {
    return contact.value;
  }

  return compactParts([
    contact.name,
    contact.title,
    contact.organization,
    contact.address1,
    contact.address2,
    contact.city,
    contact.stateOrProvince,
    contact.country,
    contact.zipCode,
    contact.email,
    contact.telephone
  ]);
}

function compactParts(parts: Array<string | null | undefined>): string {
  const value = parts
    .map((part) => part?.trim())
    .filter((part): part is string => Boolean(part))
    .join('; ');

  return value || 'n/a';
}
