import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DialogComponent } from '../../shared/dialog/dialog.component';
import {
  ConceptReportPanelComponent,
  LinkedConceptInfo,
  ReportPanelTab
} from '../../shared/concept-report-panel/concept-report-panel.component';
import { IconComponent } from '../../shared/icon/icon.component';
import { finalize, map, Observable, of, switchMap } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { formatEasternDate } from '../../core/maintenance-window-time';
import { memeAppRouteUrl } from '../../core/meme-deployment-paths';
import { rewriteMemeConceptReportLinks } from '../../core/meme-report-links';
import { ProjectContextService } from '../../core/navigation/project-context.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { OperationalApiService } from '../operations/operational-api.service';
import { OperationalProject } from '../operations/operational.models';
import {
  buildContentPfs,
  buildContentSearchPfs,
  buildWorkflowListFilterQuery,
  contentTypePath
} from './content-edit-api.helpers';
import { ContentEditApiService } from './content-edit-api.service';
import { WorkflowApiService } from './workflow-api.service';
import {
  WorkflowTrackingRecord,
  WorkflowWorklist,
  WorklistMode
} from './workflow.models';
import {
  ContentAtom,
  ContentAttribute,
  ContentComponent as ContentComponentDetail,
  ContentComponentType,
  ContentContactInfo,
  ContentCitation,
  ContentDefinition,
  ContentKeyValuePair,
  ContentMapping,
  ContentMetadata,
  ContentNote,
  ContentPrecedenceList,
  ContentRelationship,
  ContentRelationshipTypeDetail,
  ContentRootTerminology,
  ContentRouteMode,
  ContentSearchResult,
  ContentSemanticType,
  ContentSemanticTypeMetadata,
  ContentSubsetMember,
  ContentTerminology,
  ContentTermTypeDetail,
  ContentTree,
  ContentTreePosition
} from './content-edit.models';
import {
  buildAtomAddReadiness,
  buildAtomMutationReadiness,
  buildAttributeAddReadiness,
  buildAttributeMutationReadiness,
  buildConceptMutationReadiness,
  buildMergeConceptReadiness,
  buildMoveAtomsReadiness,
  buildRelationshipAddReadiness,
  buildRelationshipsAddReadiness,
  buildRelationshipMutationReadiness,
  buildSemanticTypeAddReadiness,
  buildSemanticTypeMutationReadiness,
  buildSplitConceptReadiness,
  validationBlocksCommit,
  validationErrors,
  validationNeedsWarningOverride,
  validationWarnings
} from './edit-mutation.helpers';
import { EditMutationApiService } from './edit-mutation-api.service';
import {
  EditAddAtomRequest,
  EditAddAttributeRequest,
  EditAddRelationshipRequest,
  EditAddRelationshipsRequest,
  EditAddSemanticTypeRequest,
  EditMergeConceptRequest,
  EditMoveAtomsRequest,
  EditMutationReadiness,
  EditRemoveAtomRequest,
  EditRemoveAttributeRequest,
  EditRemoveRelationshipRequest,
  EditRemoveSemanticTypeRequest,
  EditSplitConceptRequest,
  EditUpdateAtomRequest,
  EditValidationResult
} from './edit-mutation.models';

type SearchableContentType = 'CODE' | 'CONCEPT' | 'DESCRIPTOR';
type EditAccordionGroup = 'concepts' | 'metadata' | 'worklists';
type FinderDialogMode = 'concept-list' | 'merge';
type MergeTargetOption = ContentSearchResult & {
  semanticTypes?: ContentSemanticType[];
};
type MetadataContactTab = 'acquisition' | 'content' | 'license';
type MetadataCitationTab = 'Structured' | 'Raw';

interface EditPagingPreference {
  filter?: unknown;
  page?: unknown;
  pageSize?: unknown;
  sortAscending?: unknown;
  sortField?: unknown;
  typeFilter?: unknown;
}

interface EditPopoutLink {
  label: string;
  route: string;
  title: string;
  windowName: string;
  workbench: string;
}

@Component({
  selector: 'meme-content',
  imports: [FormsModule, DialogComponent, ConceptReportPanelComponent, IconComponent],
  templateUrl: './content.component.html',
  styleUrl: '../operations/operations.component.css'
})
export class ContentComponent implements OnInit {
  private readonly api = inject(ContentEditApiService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly mutationApi = inject(EditMutationApiService);
  private readonly notifications = inject(NotificationService);
  private readonly operationsApi = inject(OperationalApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly workflowApi = inject(WorkflowApiService);
  private pendingEditConceptId: number | null = null;
  private pendingEditRecordId: number | null = null;
  private pendingEditWorklistId: number | null = null;
  protected readonly projectContext = inject(ProjectContextService);

  protected readonly componentTypes: SearchableContentType[] = [
    'CONCEPT',
    'CODE',
    'DESCRIPTOR'
  ];
  protected readonly atomStatusOptions = [
    'NEEDS_REVIEW',
    'READY_FOR_PUBLICATION'
  ];
  protected readonly baseRelationshipAddTypeOptions = [
    'RO',
    'RB',
    'RN',
    'BRO',
    'BRB',
    'BRN',
    'XR'
  ];
  protected readonly baseConceptWorkflowStatusOptions = [
    'NEW',
    'EDITING_IN_PROGRESS',
    'EDITING_DONE',
    'REVIEW_NEW',
    'REVIEW_IN_PROGRESS',
    'REVIEW_DONE',
    'READY_FOR_PUBLICATION',
    'PUBLISHED',
    'NEEDS_REVIEW',
    'DEMOTION',
    'EMBRYO'
  ];
  protected readonly pageSizeOptions = [10, 25, 50];
  protected readonly sortOptions = [
    { label: 'Relevance', value: '' },
    { label: 'Name', value: 'name' }
  ];
  protected readonly editPopoutLinks: EditPopoutLink[] = [
    {
      label: 'Semantic Types',
      route: '/edit/semantic-types',
      title: 'Semantic Type Editor',
      windowName: 'styWindow',
      workbench: 'semantic-types'
    },
    {
      label: 'Code Concepts',
      route: '/edit/codeConcepts',
      title: 'Code Concepts Reference',
      windowName: 'codeConceptsWindow',
      workbench: 'code-concepts'
    },
    {
      label: 'Atoms',
      route: '/edit/atoms',
      title: 'Atoms Editor',
      windowName: 'atomWindow',
      workbench: 'atoms'
    },
    {
      label: 'Relationships',
      route: '/edit/relationships',
      title: 'Relationships Editor',
      windowName: 'relationshipWindow',
      workbench: 'relationships'
    },
    {
      label: 'Contexts',
      route: '/contexts',
      title: 'Contexts',
      windowName: 'contextWindow',
      workbench: 'contexts'
    }
  ];

  protected readonly currentTerminologies = signal<ContentTerminology[]>([]);
  protected readonly errors = signal<string[]>([]);
  protected readonly addingComponentNote = signal(false);
  protected readonly componentNoteError = signal<string | null>(null);
  protected readonly componentNoteText = signal('');
  protected readonly loadingComponent = signal(false);
  protected readonly loadingProjectContext = signal(false);
  protected readonly loadingReport = signal(false);
  protected readonly loadingReportFacets = signal(false);
  protected readonly loadingMergeTargetDetail = signal(false);
  protected readonly loadingSemanticTypeOptions = signal(false);
  protected readonly loadingTerminologies = signal(false);
  protected readonly page = signal(1);
  protected readonly pageSize = signal(10);
  protected readonly editActivityId = signal('');
  protected readonly conceptUpdateError = signal<string | null>(null);
  protected readonly conceptUpdatePublishable = signal(true);
  protected readonly conceptUpdateWorkflowStatus = signal('');
  protected readonly updatingConcept = signal(false);
  protected readonly approvalActivityId = signal('');
  protected readonly approvalResult = signal<EditValidationResult | null>(null);
  protected readonly approvingConcept = signal(false);
  protected readonly addingAtom = signal(false);
  protected readonly atomAddActivityId = signal('');
  protected readonly atomAddCodeId = signal('NOCODE');
  protected readonly atomAddConceptId = signal('');
  protected readonly atomAddDescriptorId = signal('');
  protected readonly atomAddLanguage = signal('ENG');
  protected readonly atomAddName = signal('');
  protected readonly atomAddPendingAtom = signal<ContentAtom | null>(null);
  protected readonly atomAddResult = signal<EditValidationResult | null>(null);
  protected readonly atomAddStatus = signal('NEEDS_REVIEW');
  protected readonly atomAddTermgroup = signal('');
  protected readonly atomRemovalActivityId = signal('');
  protected readonly atomRemovalPendingAtom = signal<ContentAtom | null>(null);
  protected readonly atomRemovalResult = signal<EditValidationResult | null>(null);
  protected readonly atomMoveActivityId = signal('');
  protected readonly atomMovePendingRequest =
    signal<EditMoveAtomsRequest | null>(null);
  protected readonly atomMoveResult = signal<EditValidationResult | null>(null);
  protected readonly atomMoveTargetConceptId = signal('');
  protected readonly atomMoveTargetQuery = signal('');
  protected readonly atomMoveTargetResults = signal<ContentSearchResult[]>([]);
  protected readonly atomMoveTargetSearchError = signal<string | null>(null);
  protected readonly movingAtoms = signal(false);
  protected readonly atomSplitActivityId = signal('');
  protected readonly atomSplitCopyRelated = signal(false);
  protected readonly atomSplitPendingRequest =
    signal<EditSplitConceptRequest | null>(null);
  protected readonly atomSplitRelationshipType = signal('RO');
  protected readonly atomSplitResult = signal<EditValidationResult | null>(null);
  protected readonly splittingConcept = signal(false);
  protected readonly atomUpdateActivityId = signal('');
  protected readonly atomUpdatePendingAtom = signal<ContentAtom | null>(null);
  protected readonly atomUpdateResult = signal<EditValidationResult | null>(null);
  protected readonly atomUpdateStatus = signal('NEEDS_REVIEW');
  protected readonly atomEditPendingAtom = signal<ContentAtom | null>(null);
  protected readonly atomEditPublishable = signal(false);
  protected readonly atomEditResult = signal<EditValidationResult | null>(null);
  protected readonly atomEditTarget = signal<ContentAtom | null>(null);
  protected readonly atomSimpleEditError = signal<string | null>(null);
  protected readonly atomSimpleEditLanguage = signal('');
  protected readonly atomSimpleEditName = signal('');
  protected readonly atomSimpleEditPublishable = signal(false);
  protected readonly atomSimpleEditSuppressible = signal(false);
  protected readonly atomSimpleEditTarget = signal<ContentAtom | null>(null);
  protected readonly atomSimpleEditTermgroup = signal('');
  protected readonly atomCodeConceptError = signal<string | null>(null);
  protected readonly atomCodeConceptResults = signal<ContentSearchResult[]>([]);
  protected readonly atomCodeConceptTotalCount = signal(0);
  protected readonly atomCodeConceptTarget = signal<ContentAtom | null>(null);
  protected readonly atomValidationResult = signal<EditValidationResult | null>(null);
  protected readonly atomValidationTarget = signal<ContentAtom | null>(null);
  protected readonly loadingAtomCodeConcepts = signal(false);
  protected readonly addingAttribute = signal(false);
  protected readonly attributeAddActivityId = signal('');
  protected readonly attributeAddName = signal('');
  protected readonly attributeAddPendingAttribute =
    signal<ContentAttribute | null>(null);
  protected readonly attributeAddResult = signal<EditValidationResult | null>(null);
  protected readonly attributeAddValue = signal('');
  protected readonly attributeRemovalActivityId = signal('');
  protected readonly attributeRemovalPendingAttribute =
    signal<ContentAttribute | null>(null);
  protected readonly attributeRemovalResult =
    signal<EditValidationResult | null>(null);
  protected readonly projectContextError = signal<string | null>(null);
  protected readonly projectDefaultLanguage = signal('ENG');
  protected readonly projectEditingEnabled = signal<boolean | null>(null);
  protected readonly updatingProjectEditing = signal(false);
  protected readonly projectNewAtomTermgroups = signal<string[]>([]);
  protected readonly projectValidationChecks = signal<string[]>([]);
  protected readonly mergeActivityId = signal('');
  protected readonly mergePendingTarget = signal<ContentSearchResult | null>(null);
  protected readonly mergeResult = signal<EditValidationResult | null>(null);
  protected readonly mergeReverseOrder = signal(false);
  protected readonly mergeTargetConceptId = signal('');
  protected readonly mergeTargetDetailError = signal<string | null>(null);
  protected readonly mergeTargetQuery = signal('');
  protected readonly mergeTargetCandidates = signal<MergeTargetOption[]>([]);
  protected readonly mergeTargetResults = signal<ContentSearchResult[]>([]);
  protected readonly mergeTargetSearchError = signal<string | null>(null);
  protected readonly mergingConcept = signal(false);
  protected readonly mergeDialogOpen = signal(false);
  protected readonly atomMoveDialogOpen = signal(false);
  protected readonly atomSplitDialogOpen = signal(false);
  protected readonly query = signal('');
  protected readonly results = signal<ContentSearchResult[]>([]);
  protected readonly reportDeepRelationships = signal<ContentRelationship[]>([]);
  protected readonly reportError = signal<string | null>(null);
  protected readonly reportFacetErrors = signal<string[]>([]);
  protected readonly reportHtml = signal<string | null>(null);
  protected readonly reportMappings = signal<ContentMapping[]>([]);
  protected readonly reportTrees = signal<ContentTree[]>([]);
  protected readonly contextFilter = signal('');
  protected readonly contextTreePositions = signal<ContentTreePosition[]>([]);
  protected readonly contextTreePositionCount = signal(0);
  protected readonly contextTreePositionError = signal<string | null>(null);
  protected readonly loadingContextTreePositions = signal(false);
  protected readonly searched = signal(false);
  protected readonly searching = signal(false);
  protected readonly searchingAtomMoveTargets = signal(false);
  protected readonly searchingMergeTargets = signal(false);
  protected readonly searchType = signal<SearchableContentType>('CONCEPT');
  protected readonly selectedComponent = signal<ContentComponentDetail | null>(null);
  protected readonly selectedComponentError = signal<string | null>(null);
  protected readonly selectedAtomMoveIds = signal<number[]>([]);
  protected readonly selectedAtomSplitIds = signal<number[]>([]);
  protected readonly selectedAtomMoveTarget = signal<ContentSearchResult | null>(null);
  protected readonly selectedMergeTarget = signal<ContentSearchResult | null>(null);
  protected readonly selectedResult = signal<ContentSearchResult | null>(null);

  // Concept list (working set for editing)
  protected readonly conceptList = signal<ContentComponentDetail[]>([]);
  protected readonly finderLookupId = signal('');
  protected readonly loadingFinderLookup = signal(false);
  protected readonly finderDialogOpen = signal(false);
  protected readonly finderDialogMode = signal<FinderDialogMode>('concept-list');
  protected readonly finderQuery = signal('');
  protected readonly finderResults = signal<ContentSearchResult[]>([]);
  protected readonly finderResultsTotal = signal(0);
  protected readonly finderResultsPage = signal(1);
  protected readonly finderResultsPageSize = 10;
  protected readonly loadingFinderResults = signal(false);
  protected readonly finderSelectedResult = signal<ContentSearchResult | null>(null);
  protected readonly finderPreviewConcept = signal<ContentComponentDetail | null>(null);


  protected readonly removingAttributeId = signal<number | null>(null);
  protected readonly removingAtomId = signal<number | null>(null);
  protected readonly removingComponentNoteId = signal<number | null>(null);
  protected readonly removingRelationshipId = signal<number | null>(null);
  protected readonly removingSemanticTypeId = signal<number | null>(null);
  protected readonly updatingAtomId = signal<number | null>(null);
  protected readonly addingRelationship = signal(false);
  protected readonly relationshipAddActivityId = signal('');
  protected readonly relationshipAddPendingRelationships =
    signal<ContentRelationship[] | null>(null);
  protected readonly relationshipAddPendingRelationship =
    signal<ContentRelationship | null>(null);
  protected readonly relationshipAddResult = signal<EditValidationResult | null>(null);
  protected readonly relationshipAddTargetConceptId = signal('');
  protected readonly relationshipAddType = signal('RO');
  protected readonly relationshipTargetQuery = signal('');
  protected readonly relationshipTargetResults = signal<ContentSearchResult[]>([]);
  protected readonly relationshipTargetSearchError = signal<string | null>(null);
  protected readonly searchingRelationshipTargets = signal(false);
  protected readonly selectedRelationshipTarget =
    signal<ContentSearchResult | null>(null);
  protected readonly selectedRelationshipTargets = signal<ContentSearchResult[]>([]);
  protected readonly relationshipRemovalActivityId = signal('');
  protected readonly relationshipRemovalPendingRelationship =
    signal<ContentRelationship | null>(null);
  protected readonly relationshipRemovalResult =
    signal<EditValidationResult | null>(null);
  protected readonly addingSemanticType = signal(false);
  protected readonly semanticTypeAddActivityId = signal('');
  protected readonly semanticTypeAddPendingValue = signal<string | null>(null);
  protected readonly semanticTypeAddResult = signal<EditValidationResult | null>(null);
  protected readonly semanticTypeAddValue = signal('');
  protected readonly semanticTypeOptions = signal<ContentSemanticTypeMetadata[]>([]);
  protected readonly semanticTypeOptionsError = signal<string | null>(null);
  protected readonly semanticTypeOptionsKey = signal<string | null>(null);
  protected readonly semanticTypeRemovalActivityId = signal('');
  protected readonly semanticTypeRemovalPendingType =
    signal<ContentSemanticType | null>(null);
  protected readonly semanticTypeRemovalResult =
    signal<EditValidationResult | null>(null);
  protected readonly sortAscending = signal(false);
  protected readonly sortField = signal('');
  protected readonly terminology = signal('');
  protected readonly totalCount = signal(0);
  protected readonly validatingAtomId = signal<number | null>(null);
  protected readonly version = signal('');
  protected readonly conceptValidationCheckId = signal('');
  protected readonly conceptValidationResult = signal<EditValidationResult | null>(null);
  protected readonly validatingConcept = signal(false);

  // Metadata editing
  protected readonly selectedMetadataTerminology = signal<ContentTerminology | null>(null);
  protected readonly metadata = signal<ContentMetadata | null>(null);
  protected readonly precedenceList = signal<ContentPrecedenceList | null>(null);
  protected readonly loadingMetadata = signal(false);
  protected readonly loadingPrecedenceList = signal(false);
  protected readonly metadataError = signal<string | null>(null);
  protected readonly precedenceListError = signal<string | null>(null);
  protected readonly termTypesPage = signal(1);
  protected readonly attributeNamesPage = signal(1);
  protected readonly relationshipTypesPage = signal(1);
  protected readonly additionalRelTypesPage = signal(1);
  protected readonly termTypesFilter = signal('');
  protected readonly attributeNamesFilter = signal('');
  protected readonly relationshipTypesFilter = signal('');
  protected readonly additionalRelTypesFilter = signal('');
  protected readonly removingTermTypeKey = signal<string | null>(null);
  protected readonly removingAttributeNameKey = signal<string | null>(null);
  protected readonly removingRelationshipTypeKey = signal<string | null>(null);
  protected readonly removingAdditionalRelTypeKey = signal<string | null>(null);
  protected readonly draggingPrecedenceIndex = signal<number | null>(null);
  protected readonly savingPrecedenceList = signal(false);
  protected readonly metadataPageSize = 10;

  // Term type / attribute name dialog
  protected readonly metaTermTypeDialogMode = signal<
    'addTermType' | 'editTermType' | 'addAttributeName' | 'editAttributeName' | null
  >(null);
  protected readonly metaTermTypeLoading = signal(false);
  protected readonly metaTermTypeSubmitting = signal(false);
  protected readonly metaTermTypeErrors = signal<string[]>([]);
  protected readonly metaTermTypeFormAbbreviation = signal('');
  protected readonly metaTermTypeFormExpandedForm = signal('');
  protected readonly metaTermTypeFormSuppressible = signal(false);
  protected readonly metaTermTypeFormObsolete = signal(false);
  protected readonly metaTermTypeFormHierarchicalType = signal(false);
  protected readonly metaTermTypeFormExclude = signal(false);
  protected readonly metaTermTypeFormNormExclude = signal(false);
  protected readonly metaTermTypeDetailStyle = signal<string | null>(null);
  protected readonly metaTermTypeDetailUsageType = signal<string | null>(null);
  protected readonly metaTermTypeDetailNameVariantType = signal<string | null>(null);
  protected readonly metaTermTypeDetailCodeVariantType = signal<string | null>(null);

  // Relationship type / additional rel type dialog
  protected readonly metaRelTypeDialogMode = signal<
    'addRelType' | 'editRelType' | 'addAddRelType' | 'editAddRelType' | null
  >(null);
  protected readonly metaRelTypeLoading = signal(false);
  protected readonly metaRelTypeSubmitting = signal(false);
  protected readonly metaRelTypeErrors = signal<string[]>([]);
  protected readonly metaRelTypeFormAbbreviation = signal('');
  protected readonly metaRelTypeFormExpandedForm = signal('');
  protected readonly metaRelTypeInverseFormAbbreviation = signal('');
  protected readonly metaRelTypeInverseFormExpandedForm = signal('');

  // Root terminology / terminology dialogs
  protected readonly rootTerminologyDialogOpen = signal(false);
  protected readonly rootTerminologyLoading = signal(false);
  protected readonly rootTerminologySubmitting = signal(false);
  protected readonly rootTerminologyErrors = signal<string[]>([]);
  protected readonly rootTerminologyForm = signal<ContentRootTerminology | null>(null);
  protected readonly rootTerminologyContactTab =
    signal<MetadataContactTab>('acquisition');
  protected readonly terminologyDialogOpen = signal(false);
  protected readonly terminologyLoading = signal(false);
  protected readonly terminologySubmitting = signal(false);
  protected readonly terminologyErrors = signal<string[]>([]);
  protected readonly terminologyForm = signal<ContentTerminology | null>(null);
  protected readonly terminologyCitationTab =
    signal<MetadataCitationTab>('Structured');

  // Worklists / Clusters
  protected readonly worklistsGroupOpen = signal(true);
  protected readonly conceptsGroupOpen = signal(true);
  protected readonly metadataGroupOpen = signal(true);
  protected readonly reportPanelTab = signal<ReportPanelTab>('Report');
  protected readonly worklistMode = signal<WorklistMode>('Assigned');
  protected readonly worklists = signal<WorkflowWorklist[]>([]);
  protected readonly worklistsTotalCount = signal(0);
  protected readonly selectedWorklist = signal<WorkflowWorklist | null>(null);
  protected readonly loadingWorklists = signal(false);
  protected readonly worklistPage = signal(1);
  protected readonly worklistPageSize = 10;
  protected readonly worklistFilter = signal('');
  protected readonly worklistSortField = signal<'name' | 'lastModified'>('lastModified');
  protected readonly worklistSortAsc = signal(false);
  protected readonly availableCt = signal(0);
  protected readonly assignedCt = signal(0);
  protected readonly doneCt = signal(0);
  protected readonly checklistCt = signal(0);
  protected readonly records = signal<WorkflowTrackingRecord[]>([]);
  protected readonly recordsTotalCount = signal(0);
  protected readonly selectedRecord = signal<WorkflowTrackingRecord | null>(null);
  protected readonly loadingRecords = signal(false);
  protected readonly recordsPage = signal(1);
  protected readonly recordsPageSize = signal(10);
  protected readonly recordsTypeFilter = signal('');
  protected readonly recordsFilter = signal('');

  protected readonly projectId = computed(() => this.projectContext.projectId());
  protected readonly projectRole = computed(
    () => this.projectContext.projectRole() || 'n/a'
  );
  protected readonly availableProjects = signal<OperationalProject[]>([]);
  protected readonly selectedProject = computed(() =>
    this.availableProjects().find((project) => project.id === this.projectId()) ?? null
  );
  protected readonly availableRoles = computed<string[]>(() => {
    const user = this.auth.currentUser();
    const projectId = this.projectId();
    if (!projectId) return [];
    const assigned = user.projectRoleMap?.[String(projectId)];
    if (assigned === 'ADMINISTRATOR') return ['ADMINISTRATOR', 'REVIEWER', 'AUTHOR'];
    if (assigned === 'REVIEWER') return ['REVIEWER', 'AUTHOR'];
    if (assigned === 'AUTHOR') return ['AUTHOR'];
    return assigned ? [assigned] : [];
  });
  protected readonly routeMode = computed<ContentRouteMode>(() => {
    const params = this.route.snapshot.paramMap;
    const queryParams = this.route.snapshot.queryParamMap;

    return {
      activityId: queryParams.get('activityId'),
      componentId: queryParams.get('componentId'),
      mode: params.get('mode') ?? queryParams.get('mode') ?? 'content',
      projectId: queryParams.get('projectId'),
      terminology: params.get('terminology') ?? queryParams.get('terminology'),
      terminologyId:
        params.get('terminologyId') ??
        params.get('id') ??
        queryParams.get('terminologyId') ??
        queryParams.get('id'),
      type: params.get('type') ?? queryParams.get('type'),
      version: params.get('version') ?? queryParams.get('version')
    };
  });
  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalCount() / this.pageSize()))
  );

  // Metadata paged arrays
  private filterMeta(pairs: ContentKeyValuePair[], q: string): ContentKeyValuePair[] {
    const f = q.toLowerCase().trim();
    if (!f) return pairs;
    return pairs.filter(
      (e) => (e.key ?? '').toLowerCase().includes(f) || (e.value ?? '').toLowerCase().includes(f)
    );
  }

  protected readonly filteredTermTypes = computed<ContentKeyValuePair[]>(() =>
    this.filterMeta(this.metadata()?.termTypes ?? [], this.termTypesFilter())
  );
  protected readonly pagedTermTypes = computed<ContentKeyValuePair[]>(() => {
    const start = (this.termTypesPage() - 1) * this.metadataPageSize;
    return this.filteredTermTypes().slice(start, start + this.metadataPageSize);
  });
  protected readonly termTypesTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredTermTypes().length / this.metadataPageSize))
  );

  protected readonly filteredAttributeNames = computed<ContentKeyValuePair[]>(() =>
    this.filterMeta(this.metadata()?.attributeNames ?? [], this.attributeNamesFilter())
  );
  protected readonly pagedAttributeNames = computed<ContentKeyValuePair[]>(() => {
    const start = (this.attributeNamesPage() - 1) * this.metadataPageSize;
    return this.filteredAttributeNames().slice(start, start + this.metadataPageSize);
  });
  protected readonly attributeNamesTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredAttributeNames().length / this.metadataPageSize))
  );

  protected readonly filteredRelationshipTypes = computed<ContentKeyValuePair[]>(() =>
    this.filterMeta(this.metadata()?.relationshipTypes ?? [], this.relationshipTypesFilter())
  );
  protected readonly pagedRelationshipTypes = computed<ContentKeyValuePair[]>(() => {
    const start = (this.relationshipTypesPage() - 1) * this.metadataPageSize;
    return this.filteredRelationshipTypes().slice(start, start + this.metadataPageSize);
  });
  protected readonly relationshipTypesTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredRelationshipTypes().length / this.metadataPageSize))
  );

  protected readonly filteredAdditionalRelTypes = computed<ContentKeyValuePair[]>(() =>
    this.filterMeta(this.metadata()?.additionalRelationshipTypes ?? [], this.additionalRelTypesFilter())
  );
  protected readonly pagedAdditionalRelTypes = computed<ContentKeyValuePair[]>(() => {
    const start = (this.additionalRelTypesPage() - 1) * this.metadataPageSize;
    return this.filteredAdditionalRelTypes().slice(start, start + this.metadataPageSize);
  });
  protected readonly additionalRelTypesTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredAdditionalRelTypes().length / this.metadataPageSize))
  );
  protected readonly precedenceEntries = computed(() =>
    this.precedenceList()?.precedence?.keyValuePairs ?? []
  );
  protected readonly metadataLanguageOptions = computed<ContentKeyValuePair[]>(() =>
    this.metadata()?.languages ?? []
  );
  protected readonly sortedMetadataTerminologies = computed<ContentTerminology[]>(() =>
    [...this.currentTerminologies()].sort((a, b) =>
      (a.terminology ?? '').localeCompare(b.terminology ?? '')
    )
  );

  protected readonly worklistTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.worklistsTotalCount() / this.worklistPageSize))
  );
  protected readonly recordsTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.recordsTotalCount() / this.recordsPageSize()))
  );

  protected readonly metaTermTypeDialogOpen = computed(() => this.metaTermTypeDialogMode() !== null);
  protected readonly metaTermTypeDialogTitle = computed(() => {
    const mode = this.metaTermTypeDialogMode();
    if (mode === 'addTermType') return 'Add Term Type';
    if (mode === 'editTermType') return 'Edit Term Type';
    if (mode === 'addAttributeName') return 'Add Attribute Name';
    if (mode === 'editAttributeName') return 'Edit Attribute Name';
    return '';
  });
  protected readonly metaTermTypeIsTermType = computed(() =>
    this.metaTermTypeDialogMode()?.includes('TermType') ?? false
  );
  protected readonly metaTermTypeIsEdit = computed(() =>
    this.metaTermTypeDialogMode()?.startsWith('edit') ?? false
  );
  protected readonly metaTermTypeCanSubmit = computed(() => {
    const abbrev = this.metaTermTypeFormAbbreviation().trim();
    const expanded = this.metaTermTypeFormExpandedForm().trim();
    return abbrev.length > 0 && expanded.length > 0;
  });

  protected readonly metaRelTypeDialogOpen = computed(() => this.metaRelTypeDialogMode() !== null);
  protected readonly metaRelTypeDialogTitle = computed(() => {
    const mode = this.metaRelTypeDialogMode();
    if (mode === 'addRelType') return 'Add Relationship Type';
    if (mode === 'editRelType') return 'Edit Relationship Type';
    if (mode === 'addAddRelType') return 'Add Additional Relationship Type';
    if (mode === 'editAddRelType') return 'Edit Additional Relationship Type';
    return '';
  });
  protected readonly metaRelTypeIsEdit = computed(() =>
    this.metaRelTypeDialogMode()?.startsWith('edit') ?? false
  );
  protected readonly metaRelTypeCanSubmit = computed(() => {
    const abbrev = this.metaRelTypeFormAbbreviation().trim();
    const expanded = this.metaRelTypeFormExpandedForm().trim();
    if (!abbrev || !expanded) return false;
    if (!this.metaRelTypeIsEdit()) {
      const invAbbrev = this.metaRelTypeInverseFormAbbreviation().trim();
      const invExpanded = this.metaRelTypeInverseFormExpandedForm().trim();
      if (!invAbbrev || !invExpanded) return false;
    }
    return true;
  });
  protected readonly rootTerminologyDialogTitle = computed(() => {
    const terminology = this.rootTerminologyForm()?.terminology;
    return terminology ? `Edit ${terminology} Root Terminology` : 'Edit Root Terminology';
  });
  protected readonly rootTerminologyContact = computed<ContentContactInfo>(() =>
    this.getRootTerminologyContact(this.rootTerminologyContactTab())
  );
  protected readonly rootTerminologyCanSubmit = computed(() => {
    const form = this.rootTerminologyForm();
    if (!form) return false;
    return Boolean(form.terminology?.trim());
  });
  protected readonly terminologyDialogTitle = computed(() => {
    const form = this.terminologyForm();
    return form?.terminology && form.version
      ? `Edit ${form.terminology} ${form.version}`
      : 'Edit Terminology';
  });
  protected readonly terminologyCitation = computed<ContentCitation>(() =>
    this.terminologyForm()?.citation ?? {}
  );
  protected readonly terminologyCanSubmit = computed(() => {
    const form = this.terminologyForm();
    if (!form) return false;
    return Boolean(form.terminology?.trim() && form.version?.trim());
  });

  protected readonly isReportMode = computed(() => {
    const routeMode = this.routeMode();

    return routeMode.mode !== 'content' && Boolean(routeMode.type);
  });
  protected readonly selectedLastModifiedEpoch = computed(() =>
    this.toEpochMillis(this.selectedComponent()?.lastModified)
  );
  protected readonly approvalErrors = computed(() =>
    validationErrors(this.approvalResult())
  );
  protected readonly approvalWarnings = computed(() =>
    validationWarnings(this.approvalResult())
  );
  protected readonly approvalNeedsWarningOverride = computed(() =>
    validationNeedsWarningOverride(this.approvalResult())
  );
  protected readonly atomAddErrors = computed(() =>
    validationErrors(this.atomAddResult())
  );
  protected readonly atomAddWarnings = computed(() =>
    validationWarnings(this.atomAddResult())
  );
  protected readonly atomAddComments = computed(() =>
    Array.from(this.atomAddResult()?.comments ?? [])
  );
  protected readonly atomAddNeedsWarningOverride = computed(() =>
    Boolean(this.atomAddPendingAtom()) &&
    validationNeedsWarningOverride(this.atomAddResult())
  );
  protected readonly atomAddReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildAtomAddReadiness(
      projectId,
      component?.id,
      this.atomAddName(),
      this.atomAddTermgroup(),
      this.atomAddLanguage(),
      this.atomAddCodeId(),
      this.atomAddConceptId(),
      this.atomAddDescriptorId(),
      this.mutationActivityId(this.atomAddActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (!this.projectNewAtomTermgroups().length) {
      reasons.push('Project atom termgroups are required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });
  protected readonly atomRemovalErrors = computed(() =>
    validationErrors(this.atomRemovalResult())
  );
  protected readonly atomRemovalWarnings = computed(() =>
    validationWarnings(this.atomRemovalResult())
  );
  protected readonly atomRemovalComments = computed(() =>
    Array.from(this.atomRemovalResult()?.comments ?? [])
  );
  protected readonly atomRemovalNeedsWarningOverride = computed(() =>
    Boolean(this.atomRemovalPendingAtom()) &&
    validationNeedsWarningOverride(this.atomRemovalResult())
  );
  protected readonly atomMoveErrors = computed(() =>
    validationErrors(this.atomMoveResult())
  );
  protected readonly atomMoveWarnings = computed(() =>
    validationWarnings(this.atomMoveResult())
  );
  protected readonly atomMoveComments = computed(() =>
    Array.from(this.atomMoveResult()?.comments ?? [])
  );
  protected readonly atomMoveNeedsWarningOverride = computed(() =>
    Boolean(this.atomMovePendingRequest()) &&
    validationNeedsWarningOverride(this.atomMoveResult())
  );
  protected readonly selectedAtomMoveCount = computed(
    () => this.selectedAtomMoveIds().length
  );
  protected readonly atomSplitErrors = computed(() =>
    validationErrors(this.atomSplitResult())
  );
  protected readonly atomSplitWarnings = computed(() =>
    validationWarnings(this.atomSplitResult())
  );
  protected readonly atomSplitComments = computed(() =>
    Array.from(this.atomSplitResult()?.comments ?? [])
  );
  protected readonly atomSplitNeedsWarningOverride = computed(() =>
    Boolean(this.atomSplitPendingRequest()) &&
    validationNeedsWarningOverride(this.atomSplitResult())
  );
  protected readonly selectedAtomSplitCount = computed(
    () => this.selectedAtomSplitIds().length
  );
  protected readonly atomsForMove = computed(() => {
    const ids = new Set(this.selectedAtomMoveIds());
    return (this.selectedComponent()?.atoms ?? []).filter(
      (a): a is ContentAtom => a.id != null && ids.has(a.id)
    );
  });
  protected readonly atomsForSplit = computed(() => {
    const ids = new Set(this.selectedAtomSplitIds());
    return (this.selectedComponent()?.atoms ?? []).filter(
      (a): a is ContentAtom => a.id != null && ids.has(a.id)
    );
  });
  protected readonly atomMoveReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildMoveAtomsReadiness(
      projectId,
      component?.id,
      this.parsePositiveInteger(this.atomMoveTargetConceptId()),
      this.selectedAtomMoveIds(),
      this.mutationActivityId(this.atomMoveActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });
  protected readonly atomSplitReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildSplitConceptReadiness(
      projectId,
      component?.id,
      this.selectedAtomSplitIds(),
      this.atomSplitRelationshipType(),
      this.mutationActivityId(this.atomSplitActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });
  protected readonly atomUpdateErrors = computed(() =>
    validationErrors(this.atomUpdateResult())
  );
  protected readonly atomUpdateWarnings = computed(() =>
    validationWarnings(this.atomUpdateResult())
  );
  protected readonly atomUpdateComments = computed(() =>
    Array.from(this.atomUpdateResult()?.comments ?? [])
  );
  protected readonly atomUpdateNeedsWarningOverride = computed(() =>
    Boolean(this.atomUpdatePendingAtom()) &&
    validationNeedsWarningOverride(this.atomUpdateResult())
  );
  protected readonly atomEditErrors = computed(() =>
    validationErrors(this.atomEditResult())
  );
  protected readonly atomEditWarnings = computed(() =>
    validationWarnings(this.atomEditResult())
  );
  protected readonly atomEditComments = computed(() =>
    Array.from(this.atomEditResult()?.comments ?? [])
  );
  protected readonly atomEditNeedsWarningOverride = computed(() =>
    Boolean(this.atomEditPendingAtom()) &&
    validationNeedsWarningOverride(this.atomEditResult())
  );
  protected readonly atomValidationErrors = computed(() =>
    validationErrors(this.atomValidationResult())
  );
  protected readonly atomValidationWarnings = computed(() =>
    validationWarnings(this.atomValidationResult())
  );
  protected readonly atomValidationComments = computed(() =>
    Array.from(this.atomValidationResult()?.comments ?? [])
  );
  protected readonly attributeRemovalErrors = computed(() =>
    validationErrors(this.attributeRemovalResult())
  );
  protected readonly attributeRemovalWarnings = computed(() =>
    validationWarnings(this.attributeRemovalResult())
  );
  protected readonly attributeRemovalComments = computed(() =>
    Array.from(this.attributeRemovalResult()?.comments ?? [])
  );
  protected readonly attributeRemovalNeedsWarningOverride = computed(() =>
    Boolean(this.attributeRemovalPendingAttribute()) &&
    validationNeedsWarningOverride(this.attributeRemovalResult())
  );
  protected readonly attributeAddErrors = computed(() =>
    validationErrors(this.attributeAddResult())
  );
  protected readonly attributeAddWarnings = computed(() =>
    validationWarnings(this.attributeAddResult())
  );
  protected readonly attributeAddComments = computed(() =>
    Array.from(this.attributeAddResult()?.comments ?? [])
  );
  protected readonly attributeAddNeedsWarningOverride = computed(() =>
    Boolean(this.attributeAddPendingAttribute()) &&
    validationNeedsWarningOverride(this.attributeAddResult())
  );
  protected readonly semanticTypeRemovalErrors = computed(() =>
    validationErrors(this.semanticTypeRemovalResult())
  );
  protected readonly semanticTypeRemovalWarnings = computed(() =>
    validationWarnings(this.semanticTypeRemovalResult())
  );
  protected readonly semanticTypeRemovalComments = computed(() =>
    Array.from(this.semanticTypeRemovalResult()?.comments ?? [])
  );
  protected readonly semanticTypeRemovalNeedsWarningOverride = computed(() =>
    Boolean(this.semanticTypeRemovalPendingType()) &&
    validationNeedsWarningOverride(this.semanticTypeRemovalResult())
  );
  protected readonly semanticTypeAddErrors = computed(() =>
    validationErrors(this.semanticTypeAddResult())
  );
  protected readonly semanticTypeAddWarnings = computed(() =>
    validationWarnings(this.semanticTypeAddResult())
  );
  protected readonly semanticTypeAddComments = computed(() =>
    Array.from(this.semanticTypeAddResult()?.comments ?? [])
  );
  protected readonly semanticTypeAddNeedsWarningOverride = computed(() =>
    Boolean(this.semanticTypeAddPendingValue()) &&
    validationNeedsWarningOverride(this.semanticTypeAddResult())
  );
  protected readonly atomRemovalBaseReasons = computed(() =>
    this.atomRemovalReadiness(null).reasons.filter(
      (reason) => reason !== 'Atom id is required.'
    )
  );
  protected readonly atomUpdateBaseReasons = computed(() =>
    this.atomUpdateReadiness(null).reasons.filter(
      (reason) => reason !== 'Atom id is required.'
    )
  );
  protected readonly semanticTypeRemovalBaseReasons = computed(() =>
    this.semanticTypeRemovalReadiness(null).reasons.filter(
      (reason) => reason !== 'Semantic type id is required.'
    )
  );
  protected readonly attributeRemovalBaseReasons = computed(() =>
    this.attributeRemovalReadiness(null).reasons.filter(
      (reason) => reason !== 'Attribute id is required.'
    )
  );
  protected readonly attributeAddReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildAttributeAddReadiness(
      projectId,
      component?.id,
      this.attributeAddName(),
      this.attributeAddValue(),
      this.mutationActivityId(this.attributeAddActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });
  protected readonly mergeErrors = computed(() =>
    validationErrors(this.mergeResult())
  );
  protected readonly mergeWarnings = computed(() =>
    validationWarnings(this.mergeResult())
  );
  protected readonly mergeComments = computed(() =>
    Array.from(this.mergeResult()?.comments ?? [])
  );
  protected readonly mergeTargetOptions = computed<MergeTargetOption[]>(() => {
    const selectedId = this.selectedComponent()?.id ?? null;
    const seen = new Set<number>();
    const options: MergeTargetOption[] = [];
    const addOption = (option: MergeTargetOption): void => {
      const id = option.id;
      if (!id || id === selectedId || seen.has(id)) {
        return;
      }
      seen.add(id);
      options.push(option);
    };

    this.conceptList().forEach((concept) => {
      addOption(this.toMergeTargetOption(concept));
    });
    this.mergeTargetCandidates().forEach(addOption);

    const selectedTarget = this.selectedMergeTarget();
    if (selectedTarget) {
      addOption(selectedTarget as MergeTargetOption);
    }

    return options;
  });
  protected readonly mergeFromLastModifiedEpoch = computed(() =>
    this.mergeReverseOrder()
      ? this.toEpochMillis(this.selectedMergeTarget()?.lastModified)
      : this.selectedLastModifiedEpoch()
  );
  protected readonly mergeFromLabel = computed(() =>
    this.mergeReverseOrder()
      ? this.selectedMergeTarget()?.id ||
        this.selectedMergeTarget()?.terminologyId ||
        this.mergeTargetConceptId() ||
        'target concept'
      : this.selectedComponent()?.id ||
        this.selectedComponent()?.terminologyId ||
        'selected concept'
  );
  protected readonly mergeToLabel = computed(() =>
    this.mergeReverseOrder()
      ? this.selectedComponent()?.id ||
        this.selectedComponent()?.terminologyId ||
        'selected concept'
      : this.selectedMergeTarget()?.id ||
        this.parsePositiveInteger(this.mergeTargetConceptId()) ||
        this.selectedMergeTarget()?.terminologyId ||
        'target concept'
  );
  protected readonly mergeNeedsWarningOverride = computed(() =>
    Boolean(this.mergePendingTarget()) &&
    validationNeedsWarningOverride(this.mergeResult())
  );
  protected readonly mergeReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildMergeConceptReadiness(
      projectId,
      component?.id,
      this.parsePositiveInteger(this.mergeTargetConceptId()),
      this.mutationActivityId(this.mergeActivityId()),
      this.mergeFromLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons.map((reason) =>
      reason === 'Activity id is required.'
        ? 'Select a worklist before merging.'
        : reason
    );

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }
    if (this.mergeReverseOrder()) {
      const targetConceptId = this.parsePositiveInteger(this.mergeTargetConceptId());
      const selectedTarget = this.selectedMergeTarget();

      if (!selectedTarget?.id || selectedTarget.id !== targetConceptId) {
        reasons.push('Reverse merge order requires selecting the target concept.');
      }
      if (this.loadingMergeTargetDetail()) {
        reasons.push('Target concept detail is loading.');
      }
      if (this.mergeTargetDetailError()) {
        reasons.push('Target concept detail could not be loaded.');
      }
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });
  protected readonly relationshipAddErrors = computed(() =>
    validationErrors(this.relationshipAddResult())
  );
  protected readonly relationshipAddWarnings = computed(() =>
    validationWarnings(this.relationshipAddResult())
  );
  protected readonly relationshipAddComments = computed(() =>
    Array.from(this.relationshipAddResult()?.comments ?? [])
  );
  protected readonly relationshipAddNeedsWarningOverride = computed(() =>
    Boolean(this.relationshipAddPendingRelationship()) &&
    validationNeedsWarningOverride(this.relationshipAddResult())
  );
  protected readonly relationshipBatchAddNeedsWarningOverride = computed(() =>
    Boolean(this.relationshipAddPendingRelationships()) &&
    validationNeedsWarningOverride(this.relationshipAddResult())
  );
  protected readonly relationshipBatchTargetCount = computed(
    () => this.selectedRelationshipTargets().length
  );
  protected readonly relationshipAddReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildRelationshipAddReadiness(
      projectId,
      component?.id,
      this.parsePositiveInteger(this.relationshipAddTargetConceptId()),
      this.relationshipAddType(),
      this.mutationActivityId(this.relationshipAddActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });
  protected readonly relationshipBatchAddReadiness =
    computed<EditMutationReadiness>(() => {
      const component = this.selectedComponent();
      const projectId = this.projectId();
      const projectEditingEnabled = this.projectEditingEnabled();
      const reasons = buildRelationshipsAddReadiness(
        projectId,
        component?.id,
        this.selectedRelationshipTargets().map((target) => target.id),
        this.relationshipAddType(),
        this.mutationActivityId(this.relationshipAddActivityId()),
        this.selectedLastModifiedEpoch(),
        this.projectRole(),
        projectEditingEnabled !== false
      ).reasons;

      if (!component || !this.isConceptComponent(component)) {
        reasons.push('Concept detail is required.');
      }
      if (projectId && this.loadingProjectContext()) {
        reasons.push('Project editing state is loading.');
      }
      if (projectId && this.projectContextError()) {
        reasons.push('Project editing state could not be loaded.');
      }
      if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
        reasons.push('Project editing state is required.');
      }

      return {
        canExecute: reasons.length === 0,
        reasons: Array.from(new Set(reasons))
      };
    });
  protected readonly relationshipRemovalErrors = computed(() =>
    validationErrors(this.relationshipRemovalResult())
  );
  protected readonly relationshipRemovalWarnings = computed(() =>
    validationWarnings(this.relationshipRemovalResult())
  );
  protected readonly relationshipRemovalComments = computed(() =>
    Array.from(this.relationshipRemovalResult()?.comments ?? [])
  );
  protected readonly relationshipRemovalNeedsWarningOverride = computed(() =>
    Boolean(this.relationshipRemovalPendingRelationship()) &&
    validationNeedsWarningOverride(this.relationshipRemovalResult())
  );
  protected readonly relationshipRemovalBaseReasons = computed(() =>
    this.relationshipRemovalReadiness(null).reasons.filter(
      (reason) => reason !== 'Relationship id is required.'
    )
  );
  protected readonly semanticTypeAddReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildSemanticTypeAddReadiness(
      projectId,
      component?.id,
      this.semanticTypeAddValue(),
      this.mutationActivityId(this.semanticTypeAddActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }
    if (this.loadingSemanticTypeOptions()) {
      reasons.push('Semantic type options are loading.');
    }
    if (this.semanticTypeOptionsError()) {
      reasons.push('Semantic type options could not be loaded.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });
  protected readonly conceptValidationErrors = computed(() =>
    validationErrors(this.conceptValidationResult())
  );
  protected readonly conceptValidationWarnings = computed(() =>
    validationWarnings(this.conceptValidationResult())
  );
  protected readonly conceptValidationComments = computed(() =>
    Array.from(this.conceptValidationResult()?.comments ?? [])
  );
  protected readonly canValidateConcept = computed(() => {
    const component = this.selectedComponent();

    return Boolean(
      this.projectId() &&
        component?.id &&
        this.toSearchableContentType(
          component.type || this.selectedResult()?.type || this.searchType()
        ) === 'CONCEPT'
    );
  });
  protected readonly conceptUpdateHasChanges = computed(() => {
    const component = this.selectedComponent();

    if (!component || !this.isConceptComponent(component)) {
      return false;
    }

    return (
      this.conceptUpdateWorkflowStatus().trim() !== (component.workflowStatus || '') ||
      this.conceptUpdatePublishable() !== (component.publishable !== false)
    );
  });
  protected readonly conceptUpdateReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();

    if (
      this.toSearchableContentType(
        component?.type || this.selectedResult()?.type || this.searchType()
      ) !== 'CONCEPT'
    ) {
      return {
        canExecute: false,
        reasons: ['Concept update only applies to concept detail.']
      };
    }

    const reasons = buildConceptMutationReadiness(
      projectId,
      component?.id,
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }
    if (!this.conceptUpdateWorkflowStatus().trim()) {
      reasons.push('Concept workflow status is required.');
    }
    if (!this.conceptUpdateHasChanges()) {
      reasons.push('At least one concept property must change.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });
  protected readonly componentNoteAddReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const reasons = [];

    if (!component?.id) {
      reasons.push('Persisted component detail is required.');
    }
    if (!this.componentNoteText().trim()) {
      reasons.push('Note text is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons
    };
  });
  protected readonly conceptApprovalReadiness = computed<EditMutationReadiness>(() => {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();

    if (
      this.toSearchableContentType(
        component?.type || this.selectedResult()?.type || this.searchType()
      ) !== 'CONCEPT'
    ) {
      return {
        canExecute: false,
        reasons: ['Concept approval only applies to concept detail.']
      };
    }

    const readiness = buildConceptMutationReadiness(
      projectId,
      component?.id,
      this.projectRole(),
      projectEditingEnabled !== false
    );
    const reasons = [...readiness.reasons];

    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }
    if (!this.selectedLastModifiedEpoch()) {
      reasons.push('Concept lastModified timestamp is required.');
    }
    if (!this.mutationActivityId(this.approvalActivityId())) {
      reasons.push('Activity id is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  });

  ngOnInit(): void {
    // Allow same-origin popups to retrieve the peer concept list for the Move dialog.
    (window as any).__memeGetPeerConcepts = (excludeId: number) =>
      this.conceptList().filter(c => c.id !== excludeId);

    this.destroyRef.onDestroy(() => { delete (window as any).__memeGetPeerConcepts; });

    this.restoreEditPreferences();
    this.applyRouteContext();
    this.loadProjectContext();
    this.loadRouteComponent();
    this.loadCurrentTerminologies();
    this.loadProjects();
    if (this.projectRole() !== 'ADMINISTRATOR') {
      this.loadWorklists();
      this.loadTabCounts();
    }

    const onPopupMessage = (event: MessageEvent) => {
      if (event.origin !== window.location.origin) return;
      const data = event.data as {
        type?: string;
        conceptId?: number;
        conceptIds?: number[];
        fromConceptId?: number;
        toConceptId?: number;
        newConceptId?: number
      } | null;

      if (data?.type === 'concept-approved' && data.conceptId) {
        const id = data.conceptId;
        this.conceptList.update(list =>
          list.map(c => c.id === id ? { ...c, workflowStatus: 'READY_FOR_PUBLICATION' } : c)
        );
        this.records.update(recs =>
          recs.map(r => {
            const updatedConcepts = r.concepts?.map(c =>
              c.id === id ? { ...c, workflowStatus: 'READY_FOR_PUBLICATION' } : c
            ) ?? r.concepts;
            const allApproved = updatedConcepts?.every(
              c => c.workflowStatus === 'READY_FOR_PUBLICATION'
            ) ?? false;
            return {
              ...r,
              concepts: updatedConcepts,
              workflowStatus: allApproved ? 'READY_FOR_PUBLICATION' : r.workflowStatus,
            };
          })
        );
      }

      if (data?.type === 'concept-merged' && data.fromConceptId) {
        this.refreshConceptListAfterMerge(data.fromConceptId, data.toConceptId);
        this.records.update(recs =>
          recs.filter(r => !r.concepts?.some(c => c.id === data.fromConceptId))
        );
      }

      if (data?.type === 'concept-split' && data.fromConceptId && data.newConceptId) {
        const fromId = data.fromConceptId;
        const newId = data.newConceptId;
        const fromConcept = this.conceptList().find(c => c.id === fromId);
        if (fromConcept) this.reloadConceptInList(fromConcept);
        this.api.getComponentById('concept', newId, this.projectId()).subscribe({
          next: (concept) => { if (concept) this.addConceptToList(concept); },
          error: () => {}
        });
      }

      if (data?.type === 'concept-transfer' && data.conceptIds?.length) {
        const projectId = this.projectId();
        if (!projectId) return;
        data.conceptIds.forEach((conceptId) => {
          this.api.getComponentById('concept', conceptId, projectId).subscribe({
            next: (concept) => {
              if (!concept) return;
              this.addConceptToList(concept);
              this.selectConceptFromList(concept);
            },
            error: () => {}
          });
        });
      }
    };
    window.addEventListener('message', onPopupMessage);
    this.destroyRef.onDestroy(() => window.removeEventListener('message', onPopupMessage));
  }

  protected search(): void {
    const validationErrors = this.validateSearch();

    this.errors.set(validationErrors);
    if (validationErrors.length) {
      return;
    }

    const pfs = buildContentSearchPfs(
      this.page(),
      this.pageSize(),
      this.sortField(),
      this.sortAscending(),
      this.searchType()
    );

    this.searching.set(true);
    this.api
      .findComponents(
        this.searchType(),
        this.terminology().trim(),
        this.version().trim(),
        this.query().trim(),
        pfs
      )
      .pipe(finalize(() => this.searching.set(false)))
      .subscribe({
        next: (response) => {
          this.results.set(response.items);
          this.totalCount.set(response.totalCount);
          this.searched.set(true);
          this.selectResult(response.items[0] ?? null);
        },
        error: () => {
          this.notifications.error('Content search could not be loaded.');
        }
      });
  }

  protected clearSearch(): void {
    this.query.set('');
    this.results.set([]);
    this.selectedComponent.set(null);
    this.selectedComponentError.set(null);
    this.selectedResult.set(null);
    this.totalCount.set(0);
    this.searched.set(false);
    this.errors.set([]);
  }

  protected nextPage(): void {
    if (this.page() >= this.totalPages()) {
      return;
    }

    this.page.set(this.page() + 1);
    this.search();
  }

  protected previousPage(): void {
    if (this.page() <= 1) {
      return;
    }

    this.page.set(this.page() - 1);
    this.search();
  }

  protected selectResult(result: ContentSearchResult | null): void {
    this.selectedResult.set(result);
    this.loadSelectedComponent(result);
  }

  protected setPageSize(value: string): void {
    this.pageSize.set(Number(value));
    this.page.set(1);
    if (this.searched()) {
      this.search();
    }
  }

  protected setQuery(value: string): void {
    this.query.set(value);
    this.page.set(1);
  }

  protected setSearchType(value: string): void {
    this.searchType.set(this.toSearchableContentType(value) ?? 'CONCEPT');
    this.page.set(1);
  }

  protected setSortField(value: string): void {
    this.sortField.set(value);
    this.page.set(1);
    if (this.searched()) {
      this.search();
    }
  }

  protected setSortAscending(value: boolean): void {
    this.sortAscending.set(value);
    this.page.set(1);
    if (this.searched()) {
      this.search();
    }
  }

  protected setEditActivityId(value: string): void {
    this.editActivityId.set(value);
  }

  protected setConceptUpdatePublishable(value: boolean): void {
    this.conceptUpdatePublishable.set(value);
    this.conceptUpdateError.set(null);
  }

  protected setConceptUpdateWorkflowStatus(value: string): void {
    this.conceptUpdateWorkflowStatus.set(value);
    this.conceptUpdateError.set(null);
  }

  protected activityIdValue(activityId: string): string {
    return activityId || this.editActivityId();
  }

  protected setApprovalActivityId(value: string): void {
    this.approvalActivityId.set(value);
  }

  protected setAtomAddActivityId(value: string): void {
    this.atomAddActivityId.set(value);
  }

  protected setAtomAddCodeId(value: string): void {
    this.atomAddCodeId.set(value);
  }

  protected setAtomAddConceptId(value: string): void {
    this.atomAddConceptId.set(value);
  }

  protected setAtomAddDescriptorId(value: string): void {
    this.atomAddDescriptorId.set(value);
  }

  protected setAtomAddLanguage(value: string): void {
    this.atomAddLanguage.set(value);
  }

  protected setAtomAddName(value: string): void {
    this.atomAddName.set(value);
  }

  protected setAtomAddStatus(value: string): void {
    this.atomAddStatus.set(value);
  }

  protected setAtomAddTermgroup(value: string): void {
    this.atomAddTermgroup.set(value);
  }

  protected setAtomRemovalActivityId(value: string): void {
    this.atomRemovalActivityId.set(value);
  }

  protected setAtomMoveActivityId(value: string): void {
    this.atomMoveActivityId.set(value);
    this.atomMovePendingRequest.set(null);
  }

  protected setAtomMoveTargetConceptId(value: string | number): void {
    const stringValue = String(value ?? '');
    this.atomMoveTargetConceptId.set(stringValue);
    this.atomMovePendingRequest.set(null);

    const selectedTarget = this.selectedAtomMoveTarget();
    if (
      selectedTarget?.id &&
      selectedTarget.id !== this.parsePositiveInteger(stringValue)
    ) {
      this.selectedAtomMoveTarget.set(null);
    }
  }

  protected setAtomMoveTargetQuery(value: string): void {
    this.atomMoveTargetQuery.set(value);
  }

  protected setAtomSplitActivityId(value: string): void {
    this.atomSplitActivityId.set(value);
    this.atomSplitPendingRequest.set(null);
  }

  protected setAtomSplitCopyRelated(value: boolean): void {
    this.atomSplitCopyRelated.set(value);
    this.atomSplitPendingRequest.set(null);
  }

  protected setAtomSplitRelationshipType(value: string): void {
    this.atomSplitRelationshipType.set(value);
    this.atomSplitPendingRequest.set(null);
  }

  protected setAtomUpdateActivityId(value: string): void {
    this.atomUpdateActivityId.set(value);
  }

  protected setAtomUpdateStatus(value: string): void {
    this.atomUpdateStatus.set(value);
  }

  protected setAtomEditPublishable(value: boolean): void {
    this.atomEditPublishable.set(value);
    this.atomEditPendingAtom.set(null);
  }

  protected setAtomSimpleEditLanguage(value: string): void {
    this.atomSimpleEditLanguage.set(value);
    this.atomSimpleEditError.set(null);
  }

  protected setAtomSimpleEditName(value: string): void {
    this.atomSimpleEditName.set(value);
    this.atomSimpleEditError.set(null);
  }

  protected setAtomSimpleEditPublishable(value: boolean): void {
    this.atomSimpleEditPublishable.set(value);
    this.atomSimpleEditError.set(null);
  }

  protected setAtomSimpleEditSuppressible(value: boolean): void {
    this.atomSimpleEditSuppressible.set(value);
    this.atomSimpleEditError.set(null);
  }

  protected setAtomSimpleEditTermgroup(value: string): void {
    this.atomSimpleEditTermgroup.set(value);
    this.atomSimpleEditError.set(null);
  }

  protected setAttributeAddActivityId(value: string): void {
    this.attributeAddActivityId.set(value);
  }

  protected setAttributeAddName(value: string): void {
    this.attributeAddName.set(value);
  }

  protected setAttributeAddValue(value: string): void {
    this.attributeAddValue.set(value);
  }

  protected setAttributeRemovalActivityId(value: string): void {
    this.attributeRemovalActivityId.set(value);
  }

  protected setMergeActivityId(value: string): void {
    this.mergeActivityId.set(value);
  }

  protected setMergeReverseOrder(value: boolean): void {
    this.mergeReverseOrder.set(value);
    this.mergePendingTarget.set(null);
  }

  protected setMergeTargetConceptId(value: string | number): void {
    const stringValue = String(value ?? '');
    this.mergeTargetConceptId.set(stringValue);
    this.mergePendingTarget.set(null);

    const selectedTarget = this.selectedMergeTarget();
    if (selectedTarget?.id && selectedTarget.id !== this.parsePositiveInteger(stringValue)) {
      this.selectedMergeTarget.set(null);
      this.mergeTargetDetailError.set(null);
    }
  }

  protected setMergeTargetQuery(value: string): void {
    this.mergeTargetQuery.set(value);
  }

  protected setRelationshipAddActivityId(value: string): void {
    this.relationshipAddActivityId.set(value);
    this.relationshipAddPendingRelationship.set(null);
    this.relationshipAddPendingRelationships.set(null);
  }

  protected setRelationshipAddTargetConceptId(value: string | number): void {
    const stringValue = String(value ?? '');
    this.relationshipAddTargetConceptId.set(stringValue);
    this.relationshipAddPendingRelationship.set(null);

    const selectedTarget = this.selectedRelationshipTarget();
    if (
      selectedTarget?.id &&
      selectedTarget.id !== this.parsePositiveInteger(stringValue)
    ) {
      this.selectedRelationshipTarget.set(null);
    }
  }

  protected setRelationshipAddType(value: string): void {
    this.relationshipAddType.set(value);
    this.relationshipAddPendingRelationship.set(null);
    this.relationshipAddPendingRelationships.set(null);
  }

  protected setRelationshipTargetQuery(value: string): void {
    this.relationshipTargetQuery.set(value);
  }

  protected setRelationshipRemovalActivityId(value: string): void {
    this.relationshipRemovalActivityId.set(value);
  }

  protected setSemanticTypeAddActivityId(value: string): void {
    this.semanticTypeAddActivityId.set(value);
  }

  protected setSemanticTypeAddValue(value: string): void {
    this.semanticTypeAddValue.set(value);
  }

  protected setSemanticTypeRemovalActivityId(value: string): void {
    this.semanticTypeRemovalActivityId.set(value);
  }

  protected setConceptValidationCheckId(value: string): void {
    this.conceptValidationCheckId.set(value);
  }

  protected setComponentNoteText(value: string): void {
    this.componentNoteText.set(value);
  }

  protected addNoteToSelectedComponent(): void {
    const component = this.selectedComponent();
    const componentType = this.selectedComponentType();
    const noteText = this.componentNoteText().trim();

    if (!component?.id || !componentType || !noteText) {
      return;
    }

    this.addingComponentNote.set(true);
    this.componentNoteError.set(null);
    this.api
      .addComponentNote(componentType, component.id, noteText)
      .pipe(finalize(() => this.addingComponentNote.set(false)))
      .subscribe({
        next: () => {
          this.componentNoteText.set('');
          this.notifications.success('Note added.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.componentNoteError.set('Note could not be added.');
          this.notifications.error('Note could not be added.');
        }
      });
  }

  protected canRemoveComponentNote(note: ContentNote): boolean {
    return Boolean(
      this.selectedComponent()?.id &&
        this.selectedComponentType() &&
        note.id &&
        this.removingComponentNoteId() === null
    );
  }

  protected removeNoteFromSelectedComponent(note: ContentNote): void {
    const componentType = this.selectedComponentType();

    if (!componentType || !note.id || !this.canRemoveComponentNote(note)) {
      return;
    }

    if (!window.confirm('Remove this note?')) {
      return;
    }

    this.removingComponentNoteId.set(note.id);
    this.componentNoteError.set(null);
    this.api
      .removeComponentNote(componentType, note.id)
      .pipe(finalize(() => this.removingComponentNoteId.set(null)))
      .subscribe({
        next: () => {
          this.notifications.success('Note removed.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.componentNoteError.set('Note could not be removed.');
          this.notifications.error('Note could not be removed.');
        }
      });
  }

  protected validateSelectedConcept(): void {
    const projectId = this.projectId();
    const component = this.selectedComponent();

    if (!projectId || !component || !this.canValidateConcept()) {
      return;
    }

    this.validatingConcept.set(true);
    this.conceptValidationResult.set(null);
    this.api
      .validateConcept(
        projectId,
        component,
        this.conceptValidationCheckId().trim() || null
      )
      .pipe(finalize(() => this.validatingConcept.set(false)))
      .subscribe({
        next: (result) => {
          this.conceptValidationResult.set(result);
          if (validationBlocksCommit(result)) {
            this.notifications.error('Concept validation found errors.');
            return;
          }
          this.notifications.success('Concept validation completed.');
        },
        error: () => {
          this.notifications.error('Concept validation could not be completed.');
        }
      });
  }

  protected updateSelectedConcept(): void {
    const projectId = this.projectId();
    const concept = this.buildUpdateConceptPayload();
    const conceptLabel =
      this.selectedComponent()?.terminologyId || this.selectedComponent()?.id;

    if (!projectId || !concept || !this.conceptUpdateReadiness().canExecute) {
      return;
    }

    if (!window.confirm(`Update concept "${conceptLabel}"?`)) {
      return;
    }

    this.updatingConcept.set(true);
    this.conceptUpdateError.set(null);
    this.mutationApi
      .updateConcept(projectId, concept)
      .pipe(finalize(() => this.updatingConcept.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success('Concept updated.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.conceptUpdateError.set('Concept could not be updated.');
          this.notifications.error('Concept could not be updated.');
        }
      });
  }

  protected approveSelectedConcept(overrideWarnings = false): void {
    const request = this.buildApproveConceptRequest(overrideWarnings);

    if (!request || !this.conceptApprovalReadiness().canExecute) {
      return;
    }

    const actionLabel = overrideWarnings
      ? 'Override warnings and approve'
      : 'Approve';
    const conceptLabel =
      this.selectedComponent()?.terminologyId || this.selectedComponent()?.id;

    if (!window.confirm(`${actionLabel} concept "${conceptLabel}"?`)) {
      return;
    }

    this.approvingConcept.set(true);
    this.approvalResult.set(null);
    this.mutationApi
      .approveConcept(request)
      .pipe(finalize(() => this.approvingConcept.set(false)))
      .subscribe({
        next: (result) => {
          this.approvalResult.set(result);
          if (validationBlocksCommit(result)) {
            this.notifications.error('Concept approval failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.notifications.error(
              'Concept approval returned warnings. Review and override to continue.'
            );
            return;
          }

          this.notifications.success('Concept approved.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Concept approval could not be completed.');
        }
      });
  }

  protected canValidateAtom(atom: ContentAtom): boolean {
    return Boolean(this.projectId() && atom.id && this.validatingAtomId() === null);
  }

  protected canFindAtomCodeConcepts(atom: ContentAtom): boolean {
    return Boolean(atom.codeId?.trim() && !this.loadingAtomCodeConcepts());
  }

  protected findAtomCodeConcepts(atom: ContentAtom): void {
    const codeId = atom.codeId?.trim();
    const component = this.selectedComponent();
    const terminology = component?.terminology || this.terminology();
    const version = component?.version || this.versionForTerminology(terminology);

    if (!codeId || !terminology || !version || !this.canFindAtomCodeConcepts(atom)) {
      return;
    }

    this.atomCodeConceptTarget.set(atom);
    this.atomCodeConceptResults.set([]);
    this.atomCodeConceptTotalCount.set(0);
    this.atomCodeConceptError.set(null);
    this.loadingAtomCodeConcepts.set(true);
    this.api
      .findComponents(
        'CONCEPT',
        terminology,
        version,
        `atoms.codeId:${codeId}`,
        buildContentSearchPfs(1, 25, null, false, 'CONCEPT')
      )
      .pipe(finalize(() => this.loadingAtomCodeConcepts.set(false)))
      .subscribe({
        next: (response) => {
          this.atomCodeConceptResults.set(this.sortResultsById(response.items));
          this.atomCodeConceptTotalCount.set(response.totalCount);
        },
        error: () => {
          this.atomCodeConceptError.set('Code concepts could not be loaded.');
          this.notifications.error('Code concepts could not be loaded.');
        }
      });
  }

  protected openAtomCodeConcept(result: ContentSearchResult): void {
    this.selectResult(result);
  }

  protected editPopoutUrl(
    link: EditPopoutLink,
    component: ContentComponentDetail
  ): string {
    const params = new URLSearchParams();
    const queryParams = this.buildEditPopoutQueryParams(component);

    Object.entries(queryParams).forEach(([key, value]) => {
      params.set(key, value);
    });

    return memeAppRouteUrl(link.route, params);
  }

  protected openEditPopout(
    link: EditPopoutLink,
    component: ContentComponentDetail
  ): void {
    const openedWindow = window.open(
      this.editPopoutUrl(link, component),
      link.windowName,
      'width=1200,height=800,scrollbars=yes'
    );

    if (!openedWindow) {
      return;
    }

    try {
      openedWindow.document.title = link.title;
    } catch {
      // Browser popout policies can make the opened document unavailable.
    }
    openedWindow.focus();
  }

  protected setContextFilter(value: string): void {
    this.contextFilter.set(value);
  }

  protected loadContextTreePositions(): void {
    const component = this.selectedComponent();
    const terminology = component?.terminology || this.terminology();
    const version = component?.version || this.version();
    const terminologyId = component?.terminologyId;

    if (
      !component ||
      !this.isConceptComponent(component) ||
      !terminology ||
      !version ||
      !terminologyId
    ) {
      this.contextTreePositionError.set(
        'Context browser requires a selected concept with terminology identifiers.'
      );
      return;
    }

    this.contextTreePositionError.set(null);
    this.contextTreePositions.set([]);
    this.contextTreePositionCount.set(0);
    this.loadingContextTreePositions.set(true);
    this.api
      .findDeepTreePositions(
        terminology,
        version,
        terminologyId,
        this.contextFilter().trim(),
        buildContentPfs(1, 25, 'terminology', false, '')
      )
      .pipe(finalize(() => this.loadingContextTreePositions.set(false)))
      .subscribe({
        next: (response) => {
          this.contextTreePositions.set(response.items);
          this.contextTreePositionCount.set(response.totalCount);
        },
        error: () => {
          this.contextTreePositionError.set('Contexts could not be loaded.');
          this.notifications.error('Contexts could not be loaded.');
        }
      });
  }

  protected contextTreePositionDisplay(treePosition: ContentTreePosition): string {
    return (
      [treePosition.nodeTerminologyId, treePosition.nodeName].filter(Boolean).join(' ') ||
      treePosition.terminology ||
      'n/a'
    );
  }

  protected contextTreePositionType(treePosition: ContentTreePosition): string {
    return String(treePosition.type || 'CONCEPT').toUpperCase();
  }

  protected canOpenContextTreePosition(
    treePosition: ContentTreePosition
  ): boolean {
    return Boolean(
      this.toSearchableContentType(this.contextTreePositionType(treePosition)) &&
        (treePosition.nodeTerminologyId || treePosition.nodeId)
    );
  }

  protected openContextTreePosition(treePosition: ContentTreePosition): void {
    const type = this.toSearchableContentType(this.contextTreePositionType(treePosition));

    if (!type || !this.canOpenContextTreePosition(treePosition)) {
      return;
    }

    this.selectResult({
      id: treePosition.nodeId ?? null,
      name: treePosition.nodeName ?? null,
      terminology: treePosition.nodeTerminology || treePosition.terminology,
      terminologyId: treePosition.nodeTerminologyId ?? null,
      type,
      version: treePosition.nodeVersion || treePosition.version
    });
  }

  protected isAtomSelectedForMove(atom: ContentAtom): boolean {
    return Boolean(atom.id && this.selectedAtomMoveIds().includes(atom.id));
  }

  protected setAtomSelectedForMove(atom: ContentAtom, selected: boolean): void {
    const atomId = atom.id;
    if (!atomId) {
      return;
    }

    this.selectedAtomMoveIds.update((atomIds) => {
      const existing = new Set(atomIds);
      if (selected) {
        existing.add(atomId);
      } else {
        existing.delete(atomId);
      }

      return Array.from(existing).sort((left, right) => left - right);
    });
    this.atomMovePendingRequest.set(null);
  }

  protected isAtomSelectedForSplit(atom: ContentAtom): boolean {
    return Boolean(atom.id && this.selectedAtomSplitIds().includes(atom.id));
  }

  protected setAtomSelectedForSplit(atom: ContentAtom, selected: boolean): void {
    const atomId = atom.id;
    if (!atomId) {
      return;
    }

    this.selectedAtomSplitIds.update((atomIds) => {
      const existing = new Set(atomIds);
      if (selected) {
        existing.add(atomId);
      } else {
        existing.delete(atomId);
      }

      return Array.from(existing).sort((left, right) => left - right);
    });
    this.atomSplitPendingRequest.set(null);
  }

  protected validateAtom(atom: ContentAtom): void {
    const projectId = this.projectId();

    if (!projectId || !this.canValidateAtom(atom)) {
      return;
    }

    this.validatingAtomId.set(atom.id ?? null);
    this.atomValidationTarget.set(atom);
    this.atomValidationResult.set(null);
    this.api
      .validateAtom(projectId, atom)
      .pipe(finalize(() => this.validatingAtomId.set(null)))
      .subscribe({
        next: (result) => {
          this.atomValidationResult.set(result);
          if (validationBlocksCommit(result)) {
            this.notifications.error('Atom validation found errors.');
            return;
          }
          this.notifications.success('Atom validation completed.');
        },
        error: () => {
          this.notifications.error('Atom validation could not be completed.');
        }
      });
  }

  protected addAtomToConcept(overrideWarnings = false): void {
    const request = this.buildAddAtomRequest(overrideWarnings);

    if (!request || !this.atomAddReadiness().canExecute) {
      return;
    }

    const actionLabel = overrideWarnings
      ? 'Override warnings and add'
      : 'Add';

    if (!window.confirm(`${actionLabel} atom "${request.atom.name}"?`)) {
      return;
    }

    this.addingAtom.set(true);
    this.atomAddResult.set(null);
    this.mutationApi
      .addAtomToConcept(request)
      .pipe(finalize(() => this.addingAtom.set(false)))
      .subscribe({
        next: (result) => {
          this.atomAddResult.set(result);
          if (validationBlocksCommit(result)) {
            this.atomAddPendingAtom.set(null);
            this.notifications.error('Atom add failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.atomAddPendingAtom.set(request.atom);
            this.notifications.error(
              'Atom add returned warnings. Review and override to continue.'
            );
            return;
          }

          this.atomAddPendingAtom.set(null);
          this.notifications.success('Atom added.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Atom could not be added.');
        }
      });
  }

  protected atomRemovalReadiness(atom: ContentAtom | null): EditMutationReadiness {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildAtomMutationReadiness(
      projectId,
      component?.id,
      atom?.id,
      this.mutationActivityId(this.atomRemovalActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  }

  protected canRemoveAtom(atom: ContentAtom): boolean {
    return (
      this.removingAtomId() === null &&
      this.atomRemovalReadiness(atom).canExecute
    );
  }

  protected atomUpdateReadiness(atom: ContentAtom | null): EditMutationReadiness {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildAtomMutationReadiness(
      projectId,
      component?.id,
      atom?.id,
      this.mutationActivityId(this.atomUpdateActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }
    if (!this.atomUpdateStatus().trim()) {
      reasons.push('Atom status is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  }

  protected canUpdateAtomStatus(atom: ContentAtom): boolean {
    return (
      this.updatingAtomId() === null &&
      this.atomUpdateReadiness(atom).canExecute
    );
  }

  protected atomSimpleEditReadiness(
    atom: ContentAtom | null
  ): EditMutationReadiness {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildConceptMutationReadiness(
      projectId,
      component?.id,
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (!atom?.id) {
      reasons.push('Atom id is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }
    if (!this.atomSimpleEditName().trim()) {
      reasons.push('Atom name is required.');
    }
    if (!this.atomSimpleEditLanguage().trim()) {
      reasons.push('Atom language is required.');
    }
    if (!this.parseTermgroup(this.atomSimpleEditTermgroup())) {
      reasons.push('Atom termgroup is required.');
    }
    if (atom && !this.atomSimpleEditHasChanges(atom)) {
      reasons.push('At least one atom property must change.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  }

  protected openSimpleAtomEdit(atom: ContentAtom): void {
    this.atomSimpleEditTarget.set(atom);
    this.atomSimpleEditName.set(atom.name || '');
    this.atomSimpleEditTermgroup.set(this.atomTermgroup(atom));
    this.atomSimpleEditLanguage.set(atom.language || this.projectDefaultLanguage());
    this.atomSimpleEditPublishable.set(atom.publishable !== false);
    this.atomSimpleEditSuppressible.set(atom.suppressible === true);
    this.atomSimpleEditError.set(null);
  }

  protected closeSimpleAtomEdit(): void {
    if (this.updatingAtomId() !== null) {
      return;
    }

    this.atomSimpleEditTarget.set(null);
    this.atomSimpleEditError.set(null);
  }

  protected simpleAtomTermgroupOptions(atom: ContentAtom): string[] {
    return Array.from(
      new Set(
        [this.atomTermgroup(atom), ...this.projectNewAtomTermgroups()]
          .map((termgroup) => termgroup.trim())
          .filter(Boolean)
      )
    );
  }

  protected canSimpleEditAtom(atom: ContentAtom): boolean {
    return Boolean(atom.id && this.updatingAtomId() === null);
  }

  protected updateSimpleAtom(): void {
    const targetAtom = this.atomSimpleEditTarget();
    const requestAtom = targetAtom ? this.buildSimpleAtomPayload(targetAtom) : null;
    const component = this.selectedComponent();
    const projectId = this.projectId();

    if (
      !targetAtom ||
      !requestAtom ||
      !projectId ||
      !component?.id ||
      !this.atomSimpleEditReadiness(targetAtom).canExecute
    ) {
      return;
    }

    if (!window.confirm(`Save simple atom "${this.atomDisplay(targetAtom)}"?`)) {
      return;
    }

    this.updatingAtomId.set(targetAtom.id ?? null);
    this.atomSimpleEditError.set(null);
    this.mutationApi
      .updateAtom(projectId, component.id, requestAtom)
      .pipe(finalize(() => this.updatingAtomId.set(null)))
      .subscribe({
        next: () => {
          this.atomSimpleEditTarget.set(null);
          this.notifications.success('Atom simple edit saved.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.atomSimpleEditError.set('Atom simple edit could not be saved.');
          this.notifications.error('Atom simple edit could not be saved.');
        }
      });
  }

  protected atomEditReadiness(atom: ContentAtom | null): EditMutationReadiness {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildAtomMutationReadiness(
      projectId,
      component?.id,
      atom?.id,
      this.mutationActivityId(this.atomUpdateActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  }

  protected openAtomEdit(atom: ContentAtom): void {
    this.atomEditTarget.set(atom);
    this.atomEditPublishable.set(atom.publishable !== false);
    this.atomEditPendingAtom.set(null);
    this.atomEditResult.set(null);
  }

  protected closeAtomEdit(): void {
    if (this.updatingAtomId() !== null) {
      return;
    }

    this.atomEditTarget.set(null);
    this.atomEditPendingAtom.set(null);
    this.atomEditResult.set(null);
  }

  protected canEditAtom(atom: ContentAtom): boolean {
    return Boolean(atom.id && this.updatingAtomId() === null);
  }

  protected updateAtom(atom: ContentAtom, overrideWarnings = false): void {
    const request = this.buildUpdateAtomRequest(
      atom,
      this.atomEditPublishable(),
      overrideWarnings
    );

    if (!request || !this.atomEditReadiness(atom).canExecute) {
      return;
    }

    const atomLabel = this.atomDisplay(atom);
    const actionLabel = overrideWarnings
      ? 'Override warnings and update'
      : 'Update';

    if (!window.confirm(`${actionLabel} atom "${atomLabel}"?`)) {
      return;
    }

    this.updatingAtomId.set(atom.id ?? null);
    this.atomEditResult.set(null);
    this.mutationApi
      .updateAtomOnConcept(request)
      .pipe(finalize(() => this.updatingAtomId.set(null)))
      .subscribe({
        next: (result) => {
          this.atomEditResult.set(result);
          if (validationBlocksCommit(result)) {
            this.atomEditPendingAtom.set(null);
            this.notifications.error('Atom update failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.atomEditPendingAtom.set(request.atom);
            this.notifications.error(
              'Atom update returned warnings. Review and override to continue.'
            );
            return;
          }

          this.atomEditTarget.set(null);
          this.atomEditPendingAtom.set(null);
          this.notifications.success('Atom updated.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Atom could not be updated.');
        }
      });
  }

  protected updateAtomStatus(atom: ContentAtom, overrideWarnings = false): void {
    const request = this.buildUpdateAtomStatusRequest(
      atom,
      this.atomUpdateStatus().trim(),
      overrideWarnings
    );

    if (!request || !this.atomUpdateReadiness(atom).canExecute) {
      return;
    }

    const atomLabel = this.atomDisplay(atom);
    const actionLabel = overrideWarnings
      ? 'Override warnings and update'
      : 'Update';
    const workflowStatus = request.atom.workflowStatus || this.atomUpdateStatus();

    if (!window.confirm(`${actionLabel} atom "${atomLabel}" status to ${workflowStatus}?`)) {
      return;
    }

    this.updatingAtomId.set(atom.id ?? null);
    this.atomUpdateResult.set(null);
    this.mutationApi
      .updateAtomOnConcept(request)
      .pipe(finalize(() => this.updatingAtomId.set(null)))
      .subscribe({
        next: (result) => {
          this.atomUpdateResult.set(result);
          if (validationBlocksCommit(result)) {
            this.atomUpdatePendingAtom.set(null);
            this.notifications.error('Atom status update failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.atomUpdatePendingAtom.set(request.atom);
            this.notifications.error(
              'Atom status update returned warnings. Review and override to continue.'
            );
            return;
          }

          this.atomUpdatePendingAtom.set(null);
          this.notifications.success('Atom status updated.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Atom status could not be updated.');
        }
      });
  }

  protected removeAtomFromConcept(atom: ContentAtom, overrideWarnings = false): void {
    const request = this.buildRemoveAtomRequest(atom, overrideWarnings);

    if (!request || !this.atomRemovalReadiness(atom).canExecute) {
      return;
    }

    const atomLabel = this.atomDisplay(atom);
    const actionLabel = overrideWarnings
      ? 'Override warnings and remove'
      : 'Remove';

    if (!window.confirm(`${actionLabel} atom "${atomLabel}"?`)) {
      return;
    }

    this.removingAtomId.set(atom.id ?? null);
    this.atomRemovalResult.set(null);
    this.mutationApi
      .removeAtomFromConcept(request)
      .pipe(finalize(() => this.removingAtomId.set(null)))
      .subscribe({
        next: (result) => {
          this.atomRemovalResult.set(result);
          if (validationBlocksCommit(result)) {
            this.atomRemovalPendingAtom.set(null);
            this.notifications.error('Atom removal failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.atomRemovalPendingAtom.set(atom);
            this.notifications.error(
              'Atom removal returned warnings. Review and override to continue.'
            );
            return;
          }

          this.atomRemovalPendingAtom.set(null);
          this.notifications.success('Atom removed.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Atom could not be removed.');
        }
      });
  }

  protected addAttributeToConcept(overrideWarnings = false): void {
    const request = this.buildAddAttributeRequest(overrideWarnings);

    if (!request || !this.attributeAddReadiness().canExecute) {
      return;
    }

    const label = this.attributeDisplay(request.attribute);
    const actionLabel = overrideWarnings
      ? 'Override warnings and add'
      : 'Add';

    if (!window.confirm(`${actionLabel} attribute "${label}"?`)) {
      return;
    }

    this.addingAttribute.set(true);
    this.attributeAddResult.set(null);
    this.mutationApi
      .addAttributeToConcept(request)
      .pipe(finalize(() => this.addingAttribute.set(false)))
      .subscribe({
        next: (result) => {
          this.attributeAddResult.set(result);
          if (validationBlocksCommit(result)) {
            this.attributeAddPendingAttribute.set(null);
            this.notifications.error('Attribute add failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.attributeAddPendingAttribute.set(request.attribute);
            this.notifications.error(
              'Attribute add returned warnings. Review and override to continue.'
            );
            return;
          }

          this.attributeAddPendingAttribute.set(null);
          this.notifications.success('Attribute added.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Attribute could not be added.');
        }
      });
  }

  protected attributeRemovalReadiness(
    attribute: ContentAttribute | null
  ): EditMutationReadiness {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildAttributeMutationReadiness(
      projectId,
      component?.id,
      attribute?.id,
      this.mutationActivityId(this.attributeRemovalActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  }

  protected canRemoveAttribute(attribute: ContentAttribute): boolean {
    return (
      this.removingAttributeId() === null &&
      this.attributeRemovalReadiness(attribute).canExecute
    );
  }

  protected removeAttributeFromConcept(
    attribute: ContentAttribute,
    overrideWarnings = false
  ): void {
    const request = this.buildRemoveAttributeRequest(attribute, overrideWarnings);

    if (!request || !this.attributeRemovalReadiness(attribute).canExecute) {
      return;
    }

    const label = this.attributeDisplay(attribute);
    const actionLabel = overrideWarnings
      ? 'Override warnings and remove'
      : 'Remove';

    if (!window.confirm(`${actionLabel} attribute "${label}"?`)) {
      return;
    }

    this.removingAttributeId.set(attribute.id ?? null);
    this.attributeRemovalResult.set(null);
    this.mutationApi
      .removeAttributeFromConcept(request)
      .pipe(finalize(() => this.removingAttributeId.set(null)))
      .subscribe({
        next: (result) => {
          this.attributeRemovalResult.set(result);
          if (validationBlocksCommit(result)) {
            this.attributeRemovalPendingAttribute.set(null);
            this.notifications.error('Attribute removal failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.attributeRemovalPendingAttribute.set(attribute);
            this.notifications.error(
              'Attribute removal returned warnings. Review and override to continue.'
            );
            return;
          }

          this.attributeRemovalPendingAttribute.set(null);
          this.notifications.success('Attribute removed.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Attribute could not be removed.');
        }
      });
  }

  protected semanticTypeOptionValue(option: ContentSemanticTypeMetadata): string {
    return option.expandedForm || option.abbreviation || option.typeId || '';
  }

  protected semanticTypeOptionDisplay(option: ContentSemanticTypeMetadata): string {
    return [
      option.expandedForm || option.abbreviation || option.typeId || 'n/a',
      option.typeId
    ]
      .filter(Boolean)
      .join(' ');
  }

  protected availableSemanticTypeOptions(
    component: ContentComponentDetail
  ): ContentSemanticTypeMetadata[] {
    const existing = new Set(
      (component.semanticTypes ?? [])
        .map((semanticType) => semanticType.semanticType?.trim())
        .filter((value): value is string => Boolean(value))
    );

    return this.semanticTypeOptions().filter((option) => {
      const value = this.semanticTypeOptionValue(option);

      return value && !existing.has(value);
    });
  }

  protected addSemanticTypeToConcept(overrideWarnings = false): void {
    const request = this.buildAddSemanticTypeRequest(overrideWarnings);

    if (!request || !this.semanticTypeAddReadiness().canExecute) {
      return;
    }

    const actionLabel = overrideWarnings
      ? 'Override warnings and add'
      : 'Add';

    if (!window.confirm(`${actionLabel} semantic type "${request.semanticType}"?`)) {
      return;
    }

    this.addingSemanticType.set(true);
    this.semanticTypeAddResult.set(null);
    this.mutationApi
      .addSemanticTypeToConcept(request)
      .pipe(finalize(() => this.addingSemanticType.set(false)))
      .subscribe({
        next: (result) => {
          this.semanticTypeAddResult.set(result);
          if (validationBlocksCommit(result)) {
            this.semanticTypeAddPendingValue.set(null);
            this.notifications.error('Semantic type add failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.semanticTypeAddPendingValue.set(request.semanticType);
            this.notifications.error(
              'Semantic type add returned warnings. Review and override to continue.'
            );
            return;
          }

          this.semanticTypeAddPendingValue.set(null);
          this.notifications.success('Semantic type added.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Semantic type could not be added.');
        }
      });
  }

  protected semanticTypeDisplay(semanticType: ContentSemanticType): string {
    return (
      semanticType.semanticType ||
      (semanticType.id === null || semanticType.id === undefined
        ? 'n/a'
        : `#${semanticType.id}`)
    );
  }

  protected semanticTypeRemovalReadiness(
    semanticType: ContentSemanticType | null
  ): EditMutationReadiness {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildSemanticTypeMutationReadiness(
      projectId,
      component?.id,
      semanticType?.id,
      this.mutationActivityId(this.semanticTypeRemovalActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  }

  protected canRemoveSemanticType(semanticType: ContentSemanticType): boolean {
    return (
      this.removingSemanticTypeId() === null &&
      this.semanticTypeRemovalReadiness(semanticType).canExecute
    );
  }

  protected removeSemanticTypeFromConcept(
    semanticType: ContentSemanticType,
    overrideWarnings = false
  ): void {
    const request = this.buildRemoveSemanticTypeRequest(
      semanticType,
      overrideWarnings
    );

    if (!request || !this.semanticTypeRemovalReadiness(semanticType).canExecute) {
      return;
    }

    const label = this.semanticTypeDisplay(semanticType);
    const actionLabel = overrideWarnings
      ? 'Override warnings and remove'
      : 'Remove';

    if (!window.confirm(`${actionLabel} semantic type "${label}"?`)) {
      return;
    }

    this.removingSemanticTypeId.set(semanticType.id ?? null);
    this.semanticTypeRemovalResult.set(null);
    this.mutationApi
      .removeSemanticTypeFromConcept(request)
      .pipe(finalize(() => this.removingSemanticTypeId.set(null)))
      .subscribe({
        next: (result) => {
          this.semanticTypeRemovalResult.set(result);
          if (validationBlocksCommit(result)) {
            this.semanticTypeRemovalPendingType.set(null);
            this.notifications.error('Semantic type removal failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.semanticTypeRemovalPendingType.set(semanticType);
            this.notifications.error(
              'Semantic type removal returned warnings. Review and override to continue.'
            );
            return;
          }

          this.semanticTypeRemovalPendingType.set(null);
          this.notifications.success('Semantic type removed.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Semantic type could not be removed.');
        }
      });
  }

  protected relationshipAddTypeOptions(
    component: ContentComponentDetail
  ): string[] {
    const options = [...this.baseRelationshipAddTypeOptions];

    if (component.publishable === false) {
      options.push('BBT');
    }

    return options;
  }

  protected searchAtomMoveTargets(): void {
    const component = this.selectedComponent();
    const query = this.atomMoveTargetQuery().trim();
    const terminology = component?.terminology || this.terminology();
    const version = component?.version || this.version();

    if (!query || !terminology || !version) {
      return;
    }

    this.searchingAtomMoveTargets.set(true);
    this.atomMoveTargetSearchError.set(null);
    this.atomMoveTargetResults.set([]);
    this.api
      .findComponents(
        'CONCEPT',
        terminology,
        version,
        query,
        buildContentSearchPfs(1, 5, '', false, 'CONCEPT')
      )
      .pipe(finalize(() => this.searchingAtomMoveTargets.set(false)))
      .subscribe({
        next: (state) => {
          this.atomMoveTargetResults.set(
            state.items.filter((result) => result.id !== component?.id)
          );
        },
        error: () => {
          this.atomMoveTargetSearchError.set(
            'Atom move target search could not be loaded.'
          );
        }
      });
  }

  protected selectAtomMoveTarget(result: ContentSearchResult): void {
    if (!result.id) {
      return;
    }

    this.selectedAtomMoveTarget.set(result);
    this.atomMoveTargetConceptId.set(String(result.id));
    this.atomMovePendingRequest.set(null);
  }

  protected moveSelectedAtoms(overrideWarnings = false): void {
    const request = this.buildMoveAtomsRequest(overrideWarnings);

    if (!request || !this.atomMoveReadiness().canExecute) {
      return;
    }

    const targetLabel =
      this.selectedAtomMoveTarget()?.terminologyId || request.conceptId2;
    const actionLabel = overrideWarnings ? 'Override warnings and move' : 'Move';

    if (
      !window.confirm(
        `${actionLabel} ${request.atomIds.length} atom(s) to concept "${targetLabel}"?`
      )
    ) {
      return;
    }

    this.movingAtoms.set(true);
    this.atomMoveResult.set(null);
    this.mutationApi
      .moveAtoms(request)
      .pipe(finalize(() => this.movingAtoms.set(false)))
      .subscribe({
        next: (result) => {
          this.atomMoveResult.set(result);
          if (validationBlocksCommit(result)) {
            this.atomMovePendingRequest.set(null);
            this.notifications.error('Atom move failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.atomMovePendingRequest.set(request);
            this.notifications.error(
              'Atom move returned warnings. Review and override to continue.'
            );
            return;
          }

          this.atomMovePendingRequest.set(null);
          this.selectedAtomMoveIds.set([]);
          this.atomMoveTargetConceptId.set('');
          this.selectedAtomMoveTarget.set(null);
          this.atomMoveDialogOpen.set(false);
          this.notifications.success('Atom move completed.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Atoms could not be moved.');
        }
      });
  }

  protected splitSelectedAtoms(overrideWarnings = false): void {
    const request = this.buildSplitConceptRequest(overrideWarnings);

    if (!request || !this.atomSplitReadiness().canExecute) {
      return;
    }

    const componentLabel =
      this.selectedComponent()?.terminologyId || request.conceptId;
    const actionLabel = overrideWarnings ? 'Override warnings and split' : 'Split';

    if (
      !window.confirm(
        `${actionLabel} ${request.atomIds.length} atom(s) from concept "${componentLabel}"?`
      )
    ) {
      return;
    }

    const selectedType = this.atomSplitRelationshipType().trim();
    const request$ =
      overrideWarnings && this.atomSplitPendingRequest()
        ? of(request)
        : this.api
            .getInverseRelationshipType(
              this.selectedComponent()?.terminology || this.terminology(),
              this.selectedComponent()?.version || this.version(),
              selectedType
            )
            .pipe(
              map((inverseRelationshipType) => ({
                ...request,
                relationshipType:
                  inverseRelationshipType.trim() || request.relationshipType
              }))
            );
    let submittedRequest = request;

    this.splittingConcept.set(true);
    this.atomSplitResult.set(null);
    request$
      .pipe(
        switchMap((splitRequest) => {
          submittedRequest = splitRequest;

          return this.mutationApi.splitConcept(splitRequest);
        }),
        finalize(() => this.splittingConcept.set(false))
      )
      .subscribe({
        next: (result) => {
          this.atomSplitResult.set(result);
          if (validationBlocksCommit(result)) {
            this.atomSplitPendingRequest.set(null);
            this.notifications.error('Concept split failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.atomSplitPendingRequest.set(submittedRequest);
            this.notifications.error(
              'Concept split returned warnings. Review and override to continue.'
            );
            return;
          }

          this.atomSplitPendingRequest.set(null);
          this.selectedAtomSplitIds.set([]);
          this.atomSplitDialogOpen.set(false);
          this.notifications.success('Concept split completed.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Concept could not be split.');
        }
      });
  }

  protected openMergeDialog(): void {
    this.mergeResult.set(null);
    this.mergePendingTarget.set(null);
    this.mergeReverseOrder.set(false);
    this.mergeTargetConceptId.set('');
    this.mergeTargetDetailError.set(null);
    this.mergeTargetQuery.set('');
    this.mergeTargetCandidates.set([]);
    this.mergeTargetResults.set([]);
    this.mergeTargetSearchError.set(null);
    this.selectedMergeTarget.set(null);
    this.selectDefaultMergeTarget();
    this.mergeDialogOpen.set(true);
  }

  protected closeMergeDialog(): void {
    this.mergeDialogOpen.set(false);
    this.mergeResult.set(null);
    this.mergePendingTarget.set(null);
  }

  protected openAtomMoveDialog(): void {
    this.atomMoveDialogOpen.set(true);
  }

  protected closeAtomMoveDialog(): void {
    this.atomMoveDialogOpen.set(false);
    this.atomMoveResult.set(null);
    this.atomMovePendingRequest.set(null);
  }

  protected openAtomSplitDialog(): void {
    this.atomSplitDialogOpen.set(true);
  }

  protected closeAtomSplitDialog(): void {
    this.atomSplitDialogOpen.set(false);
    this.atomSplitResult.set(null);
    this.atomSplitPendingRequest.set(null);
  }

  protected searchMergeTargets(): void {
    const component = this.selectedComponent();
    const query = this.mergeTargetQuery().trim();
    const terminology = component?.terminology || this.terminology();
    const version = component?.version || this.version();

    if (!query || !terminology || !version) {
      return;
    }

    this.searchingMergeTargets.set(true);
    this.mergeTargetSearchError.set(null);
    this.mergeTargetResults.set([]);
    this.api
      .findComponents(
        'CONCEPT',
        terminology,
        version,
        query,
        buildContentSearchPfs(1, 5, '', false, 'CONCEPT')
      )
      .pipe(finalize(() => this.searchingMergeTargets.set(false)))
      .subscribe({
        next: (state) => {
          this.mergeTargetResults.set(
            state.items.filter((result) => result.id !== component?.id)
          );
        },
        error: () => {
          this.mergeTargetSearchError.set('Merge target search could not be loaded.');
        }
      });
  }

  protected lookupMergeTargetById(): void {
    const rawId = this.mergeTargetConceptId().trim();
    if (!rawId.match(/^[1-9]\d*$/)) {
      return;
    }

    const id = Number(rawId);
    const component = this.selectedComponent();
    const projectId = this.projectId();
    if (!projectId || !component?.id) {
      return;
    }
    if (id === component.id) {
      this.mergeTargetDetailError.set('Merge target must be a different concept.');
      return;
    }

    this.loadingMergeTargetDetail.set(true);
    this.mergeTargetDetailError.set(null);
    this.api
      .getComponentById('CONCEPT', id, projectId)
      .pipe(finalize(() => this.loadingMergeTargetDetail.set(false)))
      .subscribe({
        next: (concept) => {
          if (!concept?.id) {
            this.mergeTargetDetailError.set('Target concept could not be loaded.');
            this.notifications.error('Target concept not found.');
            return;
          }

          const target = this.toMergeTargetOption(concept);
          this.addMergeTargetCandidate(target);
          this.selectMergeTarget(target);
        },
        error: () => {
          this.mergeTargetDetailError.set('Target concept could not be loaded.');
          this.notifications.error('Target concept not found.');
        }
      });
  }

  protected selectMergeTarget(result: ContentSearchResult): void {
    if (!result.id) {
      return;
    }

    this.selectedMergeTarget.set(result);
    this.mergeTargetConceptId.set(String(result.id));
    this.mergePendingTarget.set(null);
    this.mergeTargetDetailError.set(null);

    if (result.lastModified) {
      return;
    }

    this.loadingMergeTargetDetail.set(true);
    this.api
      .getComponentById('CONCEPT', result.id, this.projectId())
      .pipe(finalize(() => this.loadingMergeTargetDetail.set(false)))
      .subscribe({
        next: (component) => {
          if (this.selectedMergeTarget()?.id !== result.id) {
            return;
          }
          if (!component?.id) {
            this.mergeTargetDetailError.set('Target concept detail could not be loaded.');
            return;
          }

          const target = {
            ...result,
            ...this.toMergeTargetOption(component)
          };
          this.addMergeTargetCandidate(target);
          this.selectedMergeTarget.set(target);
        },
        error: () => {
          if (this.selectedMergeTarget()?.id === result.id) {
            this.mergeTargetDetailError.set(
              'Target concept detail could not be loaded.'
            );
          }
        }
      });
  }

  protected mergeConcept(overrideWarnings = false): void {
    const request = this.buildMergeConceptRequest(overrideWarnings);

    if (!request || !this.mergeReadiness().canExecute) {
      return;
    }

    const componentLabel = this.mergeFromLabel();
    const targetLabel = this.mergeToLabel();
    const actionLabel = overrideWarnings ? 'Override warnings and merge' : 'Merge';

    if (
      !window.confirm(
        `${actionLabel} concept "${componentLabel}" into concept "${targetLabel}"?`
      )
    ) {
      return;
    }

    this.mergingConcept.set(true);
    this.mergeResult.set(null);
    this.mutationApi
      .mergeConcepts(request)
      .pipe(finalize(() => this.mergingConcept.set(false)))
      .subscribe({
        next: (result) => {
          this.mergeResult.set(result);
          if (validationBlocksCommit(result)) {
            this.mergePendingTarget.set(null);
            this.notifications.error('Concept merge failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.mergePendingTarget.set(
              this.selectedMergeTarget() ?? { id: request.conceptId2 }
            );
            this.notifications.error(
              'Concept merge returned warnings. Review and override to continue.'
            );
            return;
          }

          this.mergePendingTarget.set(null);
          this.mergeTargetConceptId.set('');
          this.selectedMergeTarget.set(null);
          this.mergeDialogOpen.set(false);
          this.notifications.success(
            this.mergeReverseOrder()
              ? 'Reverse concept merge completed.'
              : 'Concept merged.'
          );
          this.refreshConceptListAfterMerge(request.conceptId, request.conceptId2);
        },
        error: () => {
          this.notifications.error('Concept could not be merged.');
        }
      });
  }

  protected searchRelationshipTargets(): void {
    const component = this.selectedComponent();
    const query = this.relationshipTargetQuery().trim();
    const terminology = component?.terminology || this.terminology();
    const version = component?.version || this.version();

    if (!query || !terminology || !version) {
      return;
    }

    this.searchingRelationshipTargets.set(true);
    this.relationshipTargetSearchError.set(null);
    this.relationshipTargetResults.set([]);
    this.api
      .findComponents(
        'CONCEPT',
        terminology,
        version,
        query,
        buildContentSearchPfs(1, 5, '', false, 'CONCEPT')
      )
      .pipe(finalize(() => this.searchingRelationshipTargets.set(false)))
      .subscribe({
        next: (state) => {
          this.relationshipTargetResults.set(
            state.items.filter((result) => result.id !== component?.id)
          );
        },
        error: () => {
          this.relationshipTargetSearchError.set(
            'Target concept search could not be loaded.'
          );
        }
      });
  }

  protected selectRelationshipTarget(result: ContentSearchResult): void {
    if (!result.id) {
      return;
    }

    this.selectedRelationshipTarget.set(result);
    this.relationshipAddTargetConceptId.set(String(result.id));
  }

  protected addRelationshipBatchTarget(result: ContentSearchResult): void {
    const componentId = this.selectedComponent()?.id;
    if (!result.id || result.id === componentId) {
      return;
    }

    this.selectedRelationshipTargets.update((targets) =>
      targets.some((target) => target.id === result.id)
        ? targets
        : [...targets, result]
    );
    this.relationshipAddPendingRelationships.set(null);
  }

  protected removeRelationshipBatchTarget(result: ContentSearchResult): void {
    if (!result.id) {
      return;
    }

    this.selectedRelationshipTargets.update((targets) =>
      targets.filter((target) => target.id !== result.id)
    );
    this.relationshipAddPendingRelationships.set(null);
  }

  protected isRelationshipBatchTargetSelected(
    result: ContentSearchResult
  ): boolean {
    return Boolean(
      result.id &&
        this.selectedRelationshipTargets().some((target) => target.id === result.id)
    );
  }

  protected conceptSearchTargetDisplay(result: ContentSearchResult): string {
    return (
      result.value ||
      result.name ||
      result.terminologyId ||
      (result.id === null || result.id === undefined ? 'n/a' : `#${result.id}`)
    );
  }

  protected mergeConceptName(concept: MergeTargetOption): string {
    return (
      concept.name ||
      concept.value ||
      concept.terminologyId ||
      (concept.id === null || concept.id === undefined ? 'n/a' : `#${concept.id}`)
    );
  }

  protected mergeSemanticTypeLabels(concept: MergeTargetOption): string[] {
    return (concept.semanticTypes ?? [])
      .map((semanticType) => semanticType.semanticType?.trim())
      .filter((semanticType): semanticType is string => Boolean(semanticType));
  }

  protected addRelationshipToConcept(overrideWarnings = false): void {
    const request = this.buildAddRelationshipRequest(
      overrideWarnings,
      this.relationshipAddType()
    );

    if (!request || !this.relationshipAddReadiness().canExecute) {
      return;
    }

    const selectedType = this.relationshipAddType().trim();
    const targetConceptId = request.relationship.toId;
    const actionLabel = overrideWarnings ? 'Override warnings and add' : 'Add';

    if (
      !window.confirm(
        `${actionLabel} relationship "${selectedType}" to concept #${targetConceptId}?`
      )
    ) {
      return;
    }

    const request$ =
      overrideWarnings && this.relationshipAddPendingRelationship()
        ? of(request)
        : this.api
            .getInverseRelationshipType(
              request.relationship.terminology || this.terminology(),
              request.relationship.version || this.version(),
              selectedType
            )
            .pipe(
              map((inverseRelationshipType) => ({
                ...request,
                relationship: {
                  ...request.relationship,
                  relationshipType:
                    inverseRelationshipType.trim() ||
                    request.relationship.relationshipType
                }
              }))
            );
    let submittedRelationship = request.relationship;

    this.addingRelationship.set(true);
    this.relationshipAddResult.set(null);
    request$
      .pipe(
        switchMap((relationshipRequest) => {
          submittedRelationship = relationshipRequest.relationship;

          return this.mutationApi.addRelationshipToConcept(relationshipRequest);
        }),
        finalize(() => this.addingRelationship.set(false))
      )
      .subscribe({
        next: (result) => {
          this.relationshipAddResult.set(result);
          if (validationBlocksCommit(result)) {
            this.relationshipAddPendingRelationship.set(null);
            this.notifications.error('Relationship add failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.relationshipAddPendingRelationship.set(submittedRelationship);
            this.notifications.error(
              'Relationship add returned warnings. Review and override to continue.'
            );
            return;
          }

          this.relationshipAddPendingRelationship.set(null);
          this.relationshipAddTargetConceptId.set('');
          this.notifications.success('Relationship added.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Relationship could not be added.');
        }
      });
  }

  protected addRelationshipsToConcept(overrideWarnings = false): void {
    const request = this.buildAddRelationshipsRequest(
      overrideWarnings,
      this.relationshipAddType()
    );

    if (!request || !this.relationshipBatchAddReadiness().canExecute) {
      return;
    }

    const selectedType = this.relationshipAddType().trim();
    const actionLabel = overrideWarnings ? 'Override warnings and add' : 'Add';

    if (
      !window.confirm(
        `${actionLabel} ${request.relationships.length} relationship(s) of type "${selectedType}"?`
      )
    ) {
      return;
    }

    const request$ =
      overrideWarnings && this.relationshipAddPendingRelationships()
        ? of(request)
        : this.api
            .getInverseRelationshipType(
              request.relationships[0]?.terminology || this.terminology(),
              request.relationships[0]?.version || this.version(),
              selectedType
            )
            .pipe(
              map((inverseRelationshipType) => ({
                ...request,
                relationships: request.relationships.map((relationship) => ({
                  ...relationship,
                  relationshipType:
                    inverseRelationshipType.trim() || relationship.relationshipType
                }))
              }))
            );
    let submittedRelationships = request.relationships;

    this.addingRelationship.set(true);
    this.relationshipAddResult.set(null);
    request$
      .pipe(
        switchMap((relationshipsRequest) => {
          submittedRelationships = relationshipsRequest.relationships;

          return this.mutationApi.addRelationshipsToConcept(relationshipsRequest);
        }),
        finalize(() => this.addingRelationship.set(false))
      )
      .subscribe({
        next: (result) => {
          this.relationshipAddResult.set(result);
          if (validationBlocksCommit(result)) {
            this.relationshipAddPendingRelationships.set(null);
            this.notifications.error('Relationship add failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.relationshipAddPendingRelationships.set(submittedRelationships);
            this.notifications.error(
              'Relationship add returned warnings. Review and override to continue.'
            );
            return;
          }

          this.relationshipAddPendingRelationships.set(null);
          this.selectedRelationshipTargets.set([]);
          this.notifications.success('Relationships added.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Relationships could not be added.');
        }
      });
  }

  protected relationshipRemovalReadiness(
    relationship: ContentRelationship | null
  ): EditMutationReadiness {
    const component = this.selectedComponent();
    const projectId = this.projectId();
    const projectEditingEnabled = this.projectEditingEnabled();
    const reasons = buildRelationshipMutationReadiness(
      projectId,
      component?.id,
      relationship?.id,
      this.mutationActivityId(this.relationshipRemovalActivityId()),
      this.selectedLastModifiedEpoch(),
      this.projectRole(),
      projectEditingEnabled !== false
    ).reasons;

    if (!component || !this.isConceptComponent(component)) {
      reasons.push('Concept detail is required.');
    }
    if (projectId && this.loadingProjectContext()) {
      reasons.push('Project editing state is loading.');
    }
    if (projectId && this.projectContextError()) {
      reasons.push('Project editing state could not be loaded.');
    }
    if (projectId && projectEditingEnabled === null && !this.loadingProjectContext()) {
      reasons.push('Project editing state is required.');
    }

    return {
      canExecute: reasons.length === 0,
      reasons: Array.from(new Set(reasons))
    };
  }

  protected canRemoveRelationship(relationship: ContentRelationship): boolean {
    return (
      this.removingRelationshipId() === null &&
      this.relationshipRemovalReadiness(relationship).canExecute
    );
  }

  protected removeRelationshipFromConcept(
    relationship: ContentRelationship,
    overrideWarnings = false
  ): void {
    const request = this.buildRemoveRelationshipRequest(
      relationship,
      overrideWarnings
    );

    if (!request || !this.relationshipRemovalReadiness(relationship).canExecute) {
      return;
    }

    const label = [
      this.relationshipDisplay(relationship),
      this.relationshipTargetDisplay(relationship)
    ]
      .filter(Boolean)
      .join(' ');
    const actionLabel = overrideWarnings
      ? 'Override warnings and remove'
      : 'Remove';

    if (!window.confirm(`${actionLabel} relationship "${label || relationship.id}"?`)) {
      return;
    }

    this.removingRelationshipId.set(relationship.id ?? null);
    this.relationshipRemovalResult.set(null);
    this.mutationApi
      .removeRelationshipFromConcept(request)
      .pipe(finalize(() => this.removingRelationshipId.set(null)))
      .subscribe({
        next: (result) => {
          this.relationshipRemovalResult.set(result);
          if (validationBlocksCommit(result)) {
            this.relationshipRemovalPendingRelationship.set(null);
            this.notifications.error('Relationship removal failed validation.');
            return;
          }
          if (!overrideWarnings && validationNeedsWarningOverride(result)) {
            this.relationshipRemovalPendingRelationship.set(relationship);
            this.notifications.error(
              'Relationship removal returned warnings. Review and override to continue.'
            );
            return;
          }

          this.relationshipRemovalPendingRelationship.set(null);
          this.notifications.success('Relationship removed.');
          this.loadSelectedComponent(this.selectedResult());
        },
        error: () => {
          this.notifications.error('Relationship could not be removed.');
        }
      });
  }

  protected setTerminology(value: string): void {
    this.terminology.set(value);
    const selected = this.currentTerminologies().find(
      (terminology) => terminology.terminology === value
    );

    if (selected?.version) {
      this.version.set(selected.version);
    }

    const organizingClassType = this.toSearchableContentType(
      selected?.organizingClassType
    );
    if (organizingClassType) {
      this.searchType.set(organizingClassType);
    }

    this.page.set(1);
  }

  protected setVersion(value: string): void {
    this.version.set(value);
    this.page.set(1);
  }

  protected resultDisplay(result: ContentSearchResult): string {
    return result.value || result.name || result.terminologyId || `#${result.id}`;
  }

  protected resultProperty(result: ContentSearchResult): string {
    const property = result.property;

    if (!property?.key && !property?.value) {
      return 'n/a';
    }

    return [property.key, property.value].filter(Boolean).join(': ');
  }

  private sortResultsById(
    results: readonly ContentSearchResult[]
  ): ContentSearchResult[] {
    return [...results].sort(
      (left, right) => (left.id ?? Number.MAX_SAFE_INTEGER) - (right.id ?? Number.MAX_SAFE_INTEGER)
    );
  }

  protected displayScore(score: number | null | undefined): string {
    return score === null || score === undefined ? 'n/a' : score.toFixed(2);
  }

  protected componentDisplay(component: ContentComponentDetail): string {
    return (
      component.name ||
      component.terminologyId ||
      (component.id === null || component.id === undefined ? 'n/a' : `#${component.id}`)
    );
  }

  protected componentStatus(component: ContentComponentDetail): string {
    return component.workflowStatus || (component.obsolete ? 'Obsolete' : 'Active');
  }

  protected conceptWorkflowStatusOptions(
    component: ContentComponentDetail
  ): string[] {
    const currentStatus = component.workflowStatus?.trim();

    return currentStatus &&
      !this.baseConceptWorkflowStatusOptions.includes(currentStatus)
      ? [currentStatus, ...this.baseConceptWorkflowStatusOptions]
      : this.baseConceptWorkflowStatusOptions;
  }

  protected limitedAtoms(component: ContentComponentDetail): ContentAtom[] {
    return (component.atoms ?? []).slice(0, 8);
  }

  protected limitedDefinitions(
    component: ContentComponentDetail
  ): ContentDefinition[] {
    return (component.definitions ?? []).slice(0, 5);
  }

  protected limitedSemanticTypes(
    component: ContentComponentDetail
  ): ContentSemanticType[] {
    return (component.semanticTypes ?? []).slice(0, 8);
  }

  protected limitedAttributes(
    component: ContentComponentDetail
  ): ContentAttribute[] {
    return (component.attributes ?? []).slice(0, 8);
  }

  protected limitedRelationships(
    component: ContentComponentDetail
  ): ContentRelationship[] {
    return (component.relationships ?? []).slice(0, 8);
  }

  protected limitedNotes(component: ContentComponentDetail): ContentNote[] {
    return (component.notes ?? []).slice(0, 8);
  }

  protected atomDisplay(atom: ContentAtom): string {
    return atom.name || atom.terminologyId || `#${atom.id}`;
  }

  protected atomTermgroup(atom: ContentAtom): string {
    return [atom.terminology, atom.termType].filter(Boolean).join('/');
  }

  protected definitionDisplay(definition: ContentDefinition): string {
    return definition.value || definition.atomElementStr || `#${definition.id}`;
  }

  protected definitionHtml(definition: ContentDefinition): string {
    return this.richTextHtml(this.definitionDisplay(definition));
  }

  protected definitionSource(definition: ContentDefinition): string {
    return definition.atomElementStr || definition.terminology || '';
  }

  protected definitionStatusLabels(definition: ContentDefinition): string[] {
    return [
      definition.atomElement ? 'Atom' : '',
      definition.suppressible ? 'Suppressible' : '',
      definition.obsolete ? 'Obsolete' : ''
    ].filter((label): label is string => Boolean(label));
  }

  protected attributeDisplay(attribute: ContentAttribute): string {
    return [attribute.name, attribute.value].filter(Boolean).join(': ') || `#${attribute.id}`;
  }

  protected relationshipDisplay(relationship: ContentRelationship): string {
    return [
      relationship.relationshipType,
      relationship.additionalRelationshipType
    ]
      .filter(Boolean)
      .join(' / ');
  }

  protected noteDisplay(note: ContentNote): string {
    return note.note || `#${note.id}`;
  }

  protected noteByline(note: ContentNote): string {
    return [note.lastModifiedBy, note.lastModified || note.timestamp]
      .filter(Boolean)
      .join(' ');
  }

  protected isConceptComponent(component: ContentComponentDetail): boolean {
    return (
      this.toSearchableContentType(
        component.type || this.selectedResult()?.type || this.searchType()
      ) === 'CONCEPT'
    );
  }

  protected selectedComponentType(): SearchableContentType | null {
    const component = this.selectedComponent();

    return this.toSearchableContentType(
      component?.type || this.selectedResult()?.type || this.searchType()
    );
  }

  protected reportAtoms(component: ContentComponentDetail): ContentAtom[] {
    return (component.atoms ?? []).slice(0, 25);
  }

  protected reportDefinitions(component: ContentComponentDetail): ContentDefinition[] {
    return (component.definitions ?? []).slice(0, 25);
  }

  protected reportSemanticTypes(component: ContentComponentDetail): ContentSemanticType[] {
    return (component.semanticTypes ?? []).slice(0, 25);
  }

  protected reportAttributes(component: ContentComponentDetail): ContentAttribute[] {
    return (component.attributes ?? []).slice(0, 25);
  }

  protected reportRelationships(component: ContentComponentDetail): ContentRelationship[] {
    return (component.relationships ?? []).slice(0, 25);
  }

  protected reportMembers(component: ContentComponentDetail): ContentSubsetMember[] {
    return (component.members ?? []).slice(0, 25);
  }

  protected reportNotes(component: ContentComponentDetail): ContentNote[] {
    return (component.notes ?? []).slice(0, 25);
  }

  protected treeDisplay(tree: ContentTree): string {
    return [tree.nodeTerminologyId, tree.nodeName].filter(Boolean).join(' ') || 'n/a';
  }

  protected relationshipTargetDisplay(relationship: ContentRelationship): string {
    return (
      [
        relationship.toTerminologyId,
        relationship.toName,
        relationship.to?.name
      ]
        .filter(Boolean)
        .join(' ') ||
      (relationship.toId === null || relationship.toId === undefined
        ? 'n/a'
        : `#${relationship.toId}`)
    );
  }

  protected relationshipSourceDisplay(relationship: ContentRelationship): string {
    return (
      [
        relationship.fromTerminologyId,
        relationship.fromName,
        relationship.from?.name
      ]
        .filter(Boolean)
        .join(' ') ||
      (relationship.fromId === null || relationship.fromId === undefined
        ? 'n/a'
        : `#${relationship.fromId}`)
    );
  }

  protected mappingTargetDisplay(mapping: ContentMapping): string {
    return [mapping.toTerminologyId, mapping.toName].filter(Boolean).join(' ') || 'n/a';
  }

  protected memberDisplay(member: ContentSubsetMember): string {
    return (
      member.subset?.name ||
      member.subset?.terminologyId ||
      member.member?.name ||
      member.member?.terminologyId ||
      (member.id === null || member.id === undefined ? 'n/a' : `#${member.id}`)
    );
  }

  private applyRouteContext(): void {
    const routeMode = this.routeMode();
    const routeType = this.toSearchableContentType(routeMode.type);

    if (routeType) {
      this.searchType.set(routeType);
    }
    if (routeMode.terminology) {
      this.terminology.set(routeMode.terminology);
    }
    if (routeMode.version) {
      this.version.set(routeMode.version);
    }
    if (routeMode.terminologyId) {
      this.query.set(routeMode.terminologyId);
    }
    if (routeMode.activityId) {
      this.editActivityId.set(routeMode.activityId);
    }
  }

  private loadRouteComponent(): void {
    const routeMode = this.routeMode();
    const type = this.toSearchableContentType(routeMode.type);

    if (!type || !routeMode.terminologyId) {
      return;
    }

    const routeId = Number(routeMode.terminologyId);
    const result: ContentSearchResult = {
      id: !routeMode.version && Number.isFinite(routeId) ? routeId : undefined,
      terminology: routeMode.terminology,
      terminologyId: routeMode.terminologyId,
      type,
      version: routeMode.version
    };

    this.selectResult(result);
  }

  private buildEditPopoutQueryParams(
    component: ContentComponentDetail
  ): Record<string, string> {
    const queryParams: Record<string, string> = {};
    const type = this.toSearchableContentType(component.type) ?? this.searchType();
    const values = {
      activityId: this.editActivityId().trim(),
      componentId:
        component.id === null || component.id === undefined
          ? ''
          : String(component.id),
      isChecklist: this.worklistMode() === 'Checklists' ? 'true' : '',
      projectId: this.projectId() === null ? '' : String(this.projectId()),
      recordId: this.selectedRecord()?.id ? String(this.selectedRecord()!.id!) : '',
      terminology: component.terminology || this.terminology(),
      terminologyId: component.terminologyId || '',
      type,
      version: component.version || this.version(),
      worklistId: this.selectedWorklist()?.id ? String(this.selectedWorklist()!.id!) : ''
    };

    Object.entries(values).forEach(([key, value]) => {
      if (value) {
        queryParams[key] = value;
      }
    });

    return queryParams;
  }

  private loadCurrentTerminologies(): void {
    this.loadingTerminologies.set(true);
    this.api
      .getCurrentTerminologies()
      .pipe(finalize(() => this.loadingTerminologies.set(false)))
      .subscribe({
        next: (terminologies) => {
          const current = terminologies.filter(
            (terminology) => terminology.current !== false
          );
          this.currentTerminologies.set(current);
          this.applyDefaultTerminology(current);
          if (!this.selectedMetadataTerminology() && current.length) {
            const match =
              current.find((t) => t.terminology === this.terminology()) ?? current[0];
            this.selectedMetadataTerminology.set(match);
            this.loadMetadata();
          }
        },
        error: () => {
          this.notifications.error('Current terminologies could not be loaded.');
        }
      });
  }

  private loadProjectContext(): void {
    const projectId = this.projectId();

    this.projectEditingEnabled.set(null);
    this.projectContextError.set(null);
    this.projectDefaultLanguage.set('ENG');
    this.projectNewAtomTermgroups.set([]);
    this.projectValidationChecks.set([]);
    if (!projectId) {
      return;
    }

    this.loadingProjectContext.set(true);
    this.operationsApi
      .getProject(projectId)
      .pipe(finalize(() => this.loadingProjectContext.set(false)))
      .subscribe({
        next: (project) => {
          this.projectEditingEnabled.set(project.editingEnabled === true);
          this.projectDefaultLanguage.set(project.language || 'ENG');
          this.projectNewAtomTermgroups.set(project.newAtomTermgroups ?? []);
          this.projectValidationChecks.set(project.validationChecks ?? []);
          this.applyAtomAddDefaults();
        },
        error: () => {
          this.projectContextError.set('Project context could not be loaded.');
          this.projectEditingEnabled.set(null);
          this.projectDefaultLanguage.set('ENG');
          this.projectNewAtomTermgroups.set([]);
          this.projectValidationChecks.set([]);
        }
      });
  }

  private applyDefaultTerminology(terminologies: ContentTerminology[]): void {
    if (this.terminology() && this.version()) {
      return;
    }

    const routeType = contentTypePath(this.searchType());
    const preferred =
      terminologies.find(
        (terminology) =>
          terminology.organizingClassType &&
          contentTypePath(terminology.organizingClassType) === routeType
      ) ??
      terminologies[0] ??
      null;

    if (!this.terminology() && preferred?.terminology) {
      this.terminology.set(preferred.terminology);
    }
    if (!this.version() && preferred?.version) {
      this.version.set(preferred.version);
    }
  }

  private applyAtomAddDefaults(): void {
    if (!this.atomAddLanguage()) {
      this.atomAddLanguage.set(this.projectDefaultLanguage());
    }
    if (!this.atomAddTermgroup()) {
      this.atomAddTermgroup.set(this.projectNewAtomTermgroups()[0] ?? '');
    }
  }

  private applyConceptUpdateDefaults(
    component: ContentComponentDetail | null
  ): void {
    this.conceptUpdateWorkflowStatus.set(component?.workflowStatus || '');
    this.conceptUpdatePublishable.set(component?.publishable !== false);
    this.conceptUpdateError.set(null);
  }

  private validateSearch(): string[] {
    const errors = [];

    if (!this.query().trim()) {
      errors.push('Query is required.');
    }
    if (!this.terminology().trim()) {
      errors.push('Terminology is required.');
    }
    if (!this.version().trim()) {
      errors.push('Version is required.');
    }

    return errors;
  }

  private loadSemanticTypeOptionsForComponent(
    component: ContentComponentDetail
  ): void {
    if (!this.isConceptComponent(component) || this.isReportMode()) {
      return;
    }

    const terminology = component.terminology || this.terminology();
    const version = component.version || this.version();

    if (!terminology || !version) {
      this.semanticTypeOptionsError.set(
        'Semantic type terminology context is required.'
      );
      return;
    }

    const key = `${terminology}|${version}`;
    if (this.semanticTypeOptionsKey() === key && this.semanticTypeOptions().length) {
      return;
    }

    this.semanticTypeOptionsKey.set(key);
    this.semanticTypeOptions.set([]);
    this.semanticTypeOptionsError.set(null);
    this.loadingSemanticTypeOptions.set(true);
    this.api
      .getSemanticTypes(terminology, version)
      .pipe(finalize(() => this.loadingSemanticTypeOptions.set(false)))
      .subscribe({
        next: (options) => {
          this.semanticTypeOptions.set(
            [...options].sort((left, right) =>
              this.semanticTypeOptionDisplay(left).localeCompare(
                this.semanticTypeOptionDisplay(right)
              )
            )
          );
        },
        error: () => {
          this.semanticTypeOptionsError.set('Semantic type options could not be loaded.');
        }
      });
  }

  private loadSelectedComponent(result: ContentSearchResult | null): void {
    this.selectedComponent.set(null);
    this.selectedComponentError.set(null);
    this.applyConceptUpdateDefaults(null);
    this.approvalActivityId.set('');
    this.approvalResult.set(null);
    this.componentNoteError.set(null);
    this.componentNoteText.set('');
    this.atomAddActivityId.set('');
    this.atomAddCodeId.set('NOCODE');
    this.atomAddConceptId.set('');
    this.atomAddDescriptorId.set('');
    this.atomAddLanguage.set(this.projectDefaultLanguage());
    this.atomAddName.set('');
    this.atomAddPendingAtom.set(null);
    this.atomAddResult.set(null);
    this.atomAddStatus.set('NEEDS_REVIEW');
    this.atomAddTermgroup.set(this.projectNewAtomTermgroups()[0] ?? '');
    this.atomRemovalActivityId.set('');
    this.atomRemovalPendingAtom.set(null);
    this.atomRemovalResult.set(null);
    this.atomMoveActivityId.set('');
    this.atomMovePendingRequest.set(null);
    this.atomMoveResult.set(null);
    this.atomMoveTargetConceptId.set('');
    this.atomMoveTargetQuery.set('');
    this.atomMoveTargetResults.set([]);
    this.atomMoveTargetSearchError.set(null);
    this.selectedAtomMoveIds.set([]);
    this.selectedAtomMoveTarget.set(null);
    this.atomSplitActivityId.set('');
    this.atomSplitCopyRelated.set(false);
    this.atomSplitPendingRequest.set(null);
    this.atomSplitRelationshipType.set('RO');
    this.atomSplitResult.set(null);
    this.selectedAtomSplitIds.set([]);
    this.atomUpdateActivityId.set('');
    this.atomUpdatePendingAtom.set(null);
    this.atomUpdateResult.set(null);
    this.atomUpdateStatus.set('NEEDS_REVIEW');
    this.atomEditPendingAtom.set(null);
    this.atomEditPublishable.set(false);
    this.atomEditResult.set(null);
    this.atomEditTarget.set(null);
    this.atomSimpleEditError.set(null);
    this.atomSimpleEditLanguage.set('');
    this.atomSimpleEditName.set('');
    this.atomSimpleEditPublishable.set(false);
    this.atomSimpleEditSuppressible.set(false);
    this.atomSimpleEditTarget.set(null);
    this.atomSimpleEditTermgroup.set('');
    this.atomCodeConceptError.set(null);
    this.atomCodeConceptResults.set([]);
    this.atomCodeConceptTotalCount.set(0);
    this.atomCodeConceptTarget.set(null);
    this.atomValidationResult.set(null);
    this.atomValidationTarget.set(null);
    this.attributeAddActivityId.set('');
    this.attributeAddName.set('');
    this.attributeAddPendingAttribute.set(null);
    this.attributeAddResult.set(null);
    this.attributeAddValue.set('');
    this.attributeRemovalActivityId.set('');
    this.attributeRemovalPendingAttribute.set(null);
    this.attributeRemovalResult.set(null);
    this.mergeActivityId.set('');
    this.mergePendingTarget.set(null);
    this.mergeResult.set(null);
    this.mergeReverseOrder.set(false);
    this.mergeTargetConceptId.set('');
    this.mergeTargetDetailError.set(null);
    this.mergeTargetQuery.set('');
    this.mergeTargetCandidates.set([]);
    this.mergeTargetResults.set([]);
    this.mergeTargetSearchError.set(null);
    this.selectedMergeTarget.set(null);
    this.relationshipAddActivityId.set('');
    this.relationshipAddPendingRelationships.set(null);
    this.relationshipAddPendingRelationship.set(null);
    this.relationshipAddResult.set(null);
    this.relationshipAddTargetConceptId.set('');
    this.relationshipAddType.set('RO');
    this.relationshipTargetQuery.set('');
    this.relationshipTargetResults.set([]);
    this.relationshipTargetSearchError.set(null);
    this.selectedRelationshipTarget.set(null);
    this.selectedRelationshipTargets.set([]);
    this.relationshipRemovalActivityId.set('');
    this.relationshipRemovalPendingRelationship.set(null);
    this.relationshipRemovalResult.set(null);
    this.semanticTypeAddActivityId.set('');
    this.semanticTypeAddPendingValue.set(null);
    this.semanticTypeAddResult.set(null);
    this.semanticTypeAddValue.set('');
    this.semanticTypeRemovalActivityId.set('');
    this.semanticTypeRemovalPendingType.set(null);
    this.semanticTypeRemovalResult.set(null);
    this.conceptValidationResult.set(null);
    this.loadingReport.set(false);
    this.loadingReportFacets.set(false);
    this.reportError.set(null);
    this.reportFacetErrors.set([]);
    this.reportHtml.set(null);
    this.reportDeepRelationships.set([]);
    this.reportMappings.set([]);
    this.reportTrees.set([]);
    this.contextFilter.set('');
    this.contextTreePositionError.set(null);
    this.contextTreePositions.set([]);
    this.contextTreePositionCount.set(0);
    this.applyAtomAddDefaults();

    if (!result) {
      this.loadingComponent.set(false);
      return;
    }

    const type = this.toSearchableContentType(result.type) ?? this.searchType();
    const terminology = result.terminology || this.terminology();
    const version = result.version || this.version();
    const projectId = this.projectId();
    const terminologyId = result.terminologyId;
    const request =
      terminologyId && terminology && version
        ? this.api.getComponentByTerminologyId(
            type,
            terminology,
            version,
            terminologyId,
            projectId
          )
        : type === 'CONCEPT' && result.id
          ? this.api.getComponentById(type, result.id, projectId)
          : null;

    if (!request) {
      this.selectedComponentError.set(
        'Selected component does not include enough identifiers for detail.'
      );
      return;
    }

    this.loadingComponent.set(true);
    request
      .pipe(finalize(() => this.loadingComponent.set(false)))
      .subscribe({
        next: (component) => {
          if (this.selectedResult() === result) {
            if (!component) {
              this.selectedComponentError.set(this.missingComponentMessage(result));
              return;
            }
            this.selectedComponent.set(component);
            this.applyConceptUpdateDefaults(component);
            this.loadSemanticTypeOptionsForComponent(component);
            this.loadReportForComponent(result, type, component);
            this.loadReportFacetsForComponent(result, type, component);
          }
        },
        error: () => {
          if (this.selectedResult() === result) {
            this.selectedComponentError.set('Component detail could not be loaded.');
            this.notifications.error('Component detail could not be loaded.');
          }
        }
      });
  }

  private missingComponentMessage(result: ContentSearchResult): string {
    const type = result.type || this.searchType();
    const identifier = result.terminologyId || result.id || this.query() || 'n/a';
    const terminology = result.terminology || this.terminology();
    const version = result.version || this.version();
    const terminologyContext = [terminology, version].filter(Boolean).join('/');

    return terminologyContext
      ? `No ${type} component matched ${terminologyContext}/${identifier}.`
      : `No ${type} component matched ${identifier}.`;
  }

  private loadReportFacetsForComponent(
    result: ContentSearchResult,
    type: SearchableContentType,
    component: ContentComponentDetail
  ): void {
    if (!this.isReportMode()) {
      return;
    }

    const terminology = component.terminology || result.terminology || this.terminology();
    const version = component.version || result.version || this.version();
    const terminologyId = component.terminologyId || result.terminologyId;
    if (!terminology || !version || !terminologyId) {
      this.reportFacetErrors.set([
        'Report expansion requires terminology, version, and terminology id.'
      ]);
      return;
    }

    this.loadingReportFacets.set(true);
    this.reportFacetErrors.set([]);
    const firstPage = buildContentPfs(1, 10, 'ancestorPath', true, '');
    const relationshipPage = buildContentPfs(1, 10, 'group', true, '');
    const mappingPage = buildContentPfs(1, 10, '', true, '');

    let pendingRequests = type === 'CONCEPT' ? 3 : 2;
    const finishRequest = () => {
      pendingRequests -= 1;
      if (pendingRequests <= 0 && this.selectedResult() === result) {
        this.loadingReportFacets.set(false);
      }
    };

    this.api
      .findTrees(type, terminology, version, terminologyId, firstPage)
      .pipe(finalize(finishRequest))
      .subscribe({
        next: (response) => {
          if (this.selectedResult() === result) {
            this.reportTrees.set(response.items);
          }
        },
        error: () => {
          if (this.selectedResult() === result) {
            this.addReportFacetError('Hierarchies could not be loaded.');
          }
        }
      });

    if (type === 'CONCEPT') {
      this.api
        .findDeepRelationships(terminology, version, terminologyId, relationshipPage)
        .pipe(finalize(finishRequest))
        .subscribe({
          next: (response) => {
            if (this.selectedResult() === result) {
              this.reportDeepRelationships.set(response.items);
            }
          },
          error: () => {
            if (this.selectedResult() === result) {
              this.addReportFacetError('Deep relationships could not be loaded.');
            }
          }
        });
    }

    this.api
      .findMappings(type, terminology, version, terminologyId, mappingPage)
      .pipe(finalize(finishRequest))
      .subscribe({
        next: (response) => {
          if (this.selectedResult() === result) {
            this.reportMappings.set(response.items);
          }
        },
        error: () => {
          if (this.selectedResult() === result) {
            this.addReportFacetError('Mappings could not be loaded.');
          }
        }
      });

    if (pendingRequests === 0) {
      this.loadingReportFacets.set(false);
    }
  }

  private addReportFacetError(message: string): void {
    this.reportFacetErrors.update((errors) =>
      errors.includes(message) ? errors : [...errors, message]
    );
  }

  private loadReportForComponent(
    result: ContentSearchResult,
    type: SearchableContentType,
    component: ContentComponentDetail
  ): void {
    if (!this.isReportMode()) {
      return;
    }

    if (!component.id) {
      this.reportError.set('Preformatted report requires a persisted component id.');
      return;
    }

    this.loadingReport.set(true);
    this.api.getComponentReport(type, component.id, this.projectId()).subscribe({
      next: (report) => {
        if (this.selectedResult() !== result) {
          return;
        }
        this.reportHtml.set(
          rewriteMemeConceptReportLinks(report || '', this.projectId())
        );
        this.reportError.set(null);
        this.loadingReport.set(false);
      },
      error: () => {
        if (this.selectedResult() !== result) {
          return;
        }
        this.reportError.set('Preformatted report could not be loaded.');
        this.loadingReport.set(false);
      }
    });
  }

  private toSearchableContentType(
    value: ContentComponentType | string | null | undefined
  ): SearchableContentType | null {
    const normalized = value?.trim().toUpperCase();

    return this.componentTypes.includes(normalized as SearchableContentType)
      ? (normalized as SearchableContentType)
      : null;
  }

  private toEpochMillis(value: string | number | null | undefined): number | null {
    if (typeof value === 'number') {
      return Number.isFinite(value) ? value : null;
    }

    if (!value) {
      return null;
    }

    const numeric = Number(value);
    if (Number.isFinite(numeric)) {
      return numeric;
    }

    const parsed = Date.parse(value);

    return Number.isFinite(parsed) ? parsed : null;
  }

  private richTextHtml(value: string): string {
    const normalized = value.replace(/\r\n?/g, '\n');

    if (this.hasHtmlMarkup(normalized)) {
      return normalized;
    }

    return normalized
      .split(/\n{2,}/)
      .map((paragraph) => paragraph.trim())
      .filter(Boolean)
      .map(
        (paragraph) =>
          `<p>${paragraph
            .split('\n')
            .map((line) => this.escapeHtml(line))
            .join('<br>')}</p>`
      )
      .join('');
  }

  private hasHtmlMarkup(value: string): boolean {
    return /<\/?[a-z][\s\S]*>/i.test(value);
  }

  private escapeHtml(value: string): string {
    return value.replace(/[&<>"']/g, (character) => {
      const entities: Record<string, string> = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
      };

      return entities[character];
    });
  }

  private mutationActivityId(activityId: string): string {
    return activityId.trim() || this.editActivityId().trim();
  }

  private parseTermgroup(
    termgroup: string
  ): { termType: string; terminology: string } | null {
    const value = termgroup.trim();
    const separatorIndex = value.indexOf('/');

    if (separatorIndex <= 0 || separatorIndex >= value.length - 1) {
      return null;
    }

    return {
      terminology: value.slice(0, separatorIndex),
      termType: value.slice(separatorIndex + 1)
    };
  }

  private parsePositiveInteger(value: string): number | null {
    const parsed = Number(value.trim());

    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }

  private versionForTerminology(terminology: string): string {
    return (
      this.currentTerminologies().find(
        (candidate) => candidate.terminology === terminology
      )?.version ||
      this.selectedComponent()?.version ||
      this.version()
    );
  }

  // Metadata editing

  protected openEditRootTerminologyDialog(): void {
    const terminology = this.selectedMetadataTerminology()?.terminology;
    if (!terminology) {
      this.notifications.error('Select a terminology before editing root terminology.');
      return;
    }

    this.rootTerminologyDialogOpen.set(true);
    this.rootTerminologyLoading.set(true);
    this.rootTerminologyErrors.set([]);
    this.rootTerminologyContactTab.set('acquisition');
    this.rootTerminologyForm.set(null);
    this.api
      .getRootTerminology(terminology)
      .pipe(finalize(() => this.rootTerminologyLoading.set(false)))
      .subscribe({
        next: (rootTerminology) =>
          this.rootTerminologyForm.set(
            this.normalizeRootTerminology(rootTerminology)
          ),
        error: () =>
          this.rootTerminologyErrors.set([
            'Root terminology details could not be loaded.'
          ])
      });
  }

  protected closeRootTerminologyDialog(): void {
    if (!this.rootTerminologySubmitting()) {
      this.rootTerminologyDialogOpen.set(false);
      this.rootTerminologyForm.set(null);
      this.rootTerminologyErrors.set([]);
    }
  }

  protected setRootTerminologyValue<K extends keyof ContentRootTerminology>(
    field: K,
    value: ContentRootTerminology[K]
  ): void {
    const form = this.rootTerminologyForm();
    if (!form) return;
    this.rootTerminologyForm.set({
      ...form,
      [field]: value
    });
  }

  protected setRootRestrictionLevel(value: string | number): void {
    const parsed = Number(value);
    this.setRootTerminologyValue(
      'restrictionLevel',
      Number.isFinite(parsed) ? parsed : null
    );
  }

  protected setRootTerminologyContactTab(tab: MetadataContactTab): void {
    this.rootTerminologyContactTab.set(tab);
  }

  protected setRootTerminologyContactValue(
    field: keyof ContentContactInfo,
    value: string
  ): void {
    const form = this.rootTerminologyForm();
    if (!form) return;

    const contactKey = this.rootTerminologyContactKey(
      this.rootTerminologyContactTab()
    );
    const contact = this.getRootTerminologyContact(
      this.rootTerminologyContactTab()
    );

    this.rootTerminologyForm.set({
      ...form,
      [contactKey]: {
        ...contact,
        [field]: value
      }
    });
  }

  protected submitRootTerminologyDialog(): void {
    const form = this.rootTerminologyForm();
    if (!form || !this.rootTerminologyCanSubmit()) return;

    this.rootTerminologySubmitting.set(true);
    this.rootTerminologyErrors.set([]);
    this.api
      .updateRootTerminology(form)
      .pipe(finalize(() => this.rootTerminologySubmitting.set(false)))
      .subscribe({
        next: () => {
          this.rootTerminologyDialogOpen.set(false);
          this.rootTerminologyForm.set(null);
          this.loadCurrentTerminologies();
        },
        error: () =>
          this.rootTerminologyErrors.set([
            'Root terminology details could not be saved.'
          ])
      });
  }

  protected openEditTerminologyDialog(): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t.version) {
      this.notifications.error('Select a terminology before editing terminology.');
      return;
    }

    this.terminologyDialogOpen.set(true);
    this.terminologyLoading.set(true);
    this.terminologyErrors.set([]);
    this.terminologyCitationTab.set('Structured');
    this.terminologyForm.set(null);
    this.api
      .getTerminology(t.terminology, t.version)
      .pipe(finalize(() => this.terminologyLoading.set(false)))
      .subscribe({
        next: (terminology) =>
          this.terminologyForm.set(this.normalizeTerminology(terminology)),
        error: () =>
          this.terminologyErrors.set(['Terminology details could not be loaded.'])
      });
  }

  protected closeTerminologyDialog(): void {
    if (!this.terminologySubmitting()) {
      this.terminologyDialogOpen.set(false);
      this.terminologyForm.set(null);
      this.terminologyErrors.set([]);
    }
  }

  protected setTerminologyValue<K extends keyof ContentTerminology>(
    field: K,
    value: ContentTerminology[K]
  ): void {
    const form = this.terminologyForm();
    if (!form) return;
    this.terminologyForm.set({
      ...form,
      [field]: value
    });
  }

  protected setTerminologyCitationTab(tab: MetadataCitationTab): void {
    this.terminologyCitationTab.set(tab);
  }

  protected setTerminologyCitationValue(
    field: keyof ContentCitation,
    value: string
  ): void {
    const form = this.terminologyForm();
    if (!form) return;

    this.terminologyForm.set({
      ...form,
      citation: {
        ...this.emptyCitation(),
        ...(form.citation ?? {}),
        [field]: value
      }
    });
  }

  protected submitTerminologyDialog(): void {
    const form = this.terminologyForm();
    if (!form || !this.terminologyCanSubmit()) return;

    this.terminologySubmitting.set(true);
    this.terminologyErrors.set([]);
    this.api
      .updateTerminology(form)
      .pipe(finalize(() => this.terminologySubmitting.set(false)))
      .subscribe({
        next: () => {
          this.terminologyDialogOpen.set(false);
          this.terminologyForm.set(null);
          this.loadCurrentTerminologies();
        },
        error: () =>
          this.terminologyErrors.set(['Terminology details could not be saved.'])
      });
  }

  private normalizeRootTerminology(
    rootTerminology: ContentRootTerminology
  ): ContentRootTerminology {
    return {
      ...rootTerminology,
      acquisitionContact: this.normalizeContact(rootTerminology.acquisitionContact),
      contentContact: this.normalizeContact(rootTerminology.contentContact),
      family: rootTerminology.family ?? '',
      hierarchicalName: rootTerminology.hierarchicalName ?? '',
      hierarchyComputable: rootTerminology.hierarchyComputable !== false,
      language: rootTerminology.language ?? '',
      licenseContact: this.normalizeContact(rootTerminology.licenseContact),
      preferredName: rootTerminology.preferredName ?? '',
      restrictionLevel: rootTerminology.restrictionLevel ?? 0,
      shortName: rootTerminology.shortName ?? '',
      synonymousNames: rootTerminology.synonymousNames ?? [],
      terminology: rootTerminology.terminology ?? ''
    };
  }

  private normalizeTerminology(
    terminology: ContentTerminology
  ): ContentTerminology {
    return {
      ...terminology,
      assertsRelDirection: terminology.assertsRelDirection === true,
      citation: {
        ...this.emptyCitation(),
        ...(terminology.citation ?? {})
      },
      current: terminology.current === true,
      includeSiblings: terminology.includeSiblings === true,
      inverterEmail: terminology.inverterEmail ?? '',
      organizingClassType: terminology.organizingClassType ?? '',
      preferredName: terminology.preferredName ?? '',
      terminology: terminology.terminology ?? '',
      version: terminology.version ?? ''
    };
  }

  private normalizeContact(contact?: ContentContactInfo | null): ContentContactInfo {
    return {
      ...this.emptyContact(),
      ...(contact ?? {})
    };
  }

  private emptyContact(): ContentContactInfo {
    return {
      address1: '',
      address2: '',
      city: '',
      country: '',
      email: '',
      fax: '',
      name: '',
      organization: '',
      stateOrProvince: '',
      telephone: '',
      title: '',
      url: '',
      zipCode: ''
    };
  }

  private emptyCitation(): ContentCitation {
    return {
      author: '',
      availabilityStatement: '',
      contentDesignator: '',
      dateOfRevision: '',
      edition: '',
      editor: '',
      extent: '',
      location: '',
      notes: '',
      organization: '',
      placeOfPublication: '',
      series: '',
      title: '',
      unstructuredValue: ''
    };
  }

  private getRootTerminologyContact(tab: MetadataContactTab): ContentContactInfo {
    const form = this.rootTerminologyForm();
    if (!form) return {};
    const contact = form[this.rootTerminologyContactKey(tab)];
    return contact ?? {};
  }

  private rootTerminologyContactKey(
    tab: MetadataContactTab
  ): 'acquisitionContact' | 'contentContact' | 'licenseContact' {
    if (tab === 'content') return 'contentContact';
    if (tab === 'license') return 'licenseContact';
    return 'acquisitionContact';
  }

  protected selectMetadataTerminology(terminologyAbbrev: string): void {
    const found = this.currentTerminologies().find(
      (t) => t.terminology === terminologyAbbrev
    ) ?? null;
    this.selectedMetadataTerminology.set(found);
    this.loadMetadata();
  }

  private loadMetadata(): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) {
      this.precedenceList.set(null);
      this.precedenceListError.set(null);
      return;
    }
    this.loadingMetadata.set(true);
    this.metadataError.set(null);
    this.loadPrecedenceList(t.terminology, t.version);
    this.api
      .getAllMetadata(t.terminology, t.version)
      .pipe(finalize(() => this.loadingMetadata.set(false)))
      .subscribe({
        next: (meta) => {
          this.metadata.set(meta);
          this.termTypesPage.set(1);
          this.attributeNamesPage.set(1);
          this.relationshipTypesPage.set(1);
          this.additionalRelTypesPage.set(1);
          this.termTypesFilter.set('');
          this.attributeNamesFilter.set('');
          this.relationshipTypesFilter.set('');
          this.additionalRelTypesFilter.set('');
        },
        error: () => this.metadataError.set('Metadata could not be loaded.')
      });
  }

  private loadPrecedenceList(
    terminology: string,
    version: string,
    preserveError = false,
    keepExistingList = false
  ): void {
    if (!keepExistingList) {
      this.precedenceList.set(null);
    }
    if (!preserveError) {
      this.precedenceListError.set(null);
    }
    if (!keepExistingList) {
      this.loadingPrecedenceList.set(true);
    }
    this.api
      .getDefaultPrecedenceList(terminology, version)
      .pipe(finalize(() => this.loadingPrecedenceList.set(false)))
      .subscribe({
        next: (list) => {
          this.precedenceList.set(list);
          if (!list) {
            this.precedenceListError.set('Precedence list could not be found.');
          } else if (!preserveError) {
            this.precedenceListError.set(null);
          }
        },
        error: () => {
          if (!preserveError) {
            this.precedenceListError.set('Precedence list could not be loaded.');
          }
        }
      });
  }

  protected precedenceEntryLabel(entry: ContentKeyValuePair): string {
    const terminology = entry.key || 'n/a';
    const termType = entry.value || 'n/a';

    return `${terminology}/${termType}`;
  }

  protected precedenceEntryTrack(entry: ContentKeyValuePair, _index: number): string {
    return `${entry.key ?? ''}/${entry.value ?? ''}`;
  }

  protected startPrecedenceDrag(event: DragEvent, index: number): void {
    if (this.savingPrecedenceList()) {
      event.preventDefault();
      return;
    }

    this.draggingPrecedenceIndex.set(index);
    event.dataTransfer?.setData('text/plain', String(index));
    event.dataTransfer?.setDragImage(event.currentTarget as Element, 0, 0);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  protected dragOverPrecedenceEntry(event: DragEvent, index: number): void {
    const sourceIndex = this.draggingPrecedenceIndex();

    if (sourceIndex !== null && sourceIndex !== index && !this.savingPrecedenceList()) {
      event.preventDefault();
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move';
      }
    }
  }

  protected dropPrecedenceEntry(event: DragEvent, targetIndex: number): void {
    const rawSourceIndex =
      this.draggingPrecedenceIndex() ??
      Number(event.dataTransfer?.getData('text/plain'));
    const sourceIndex = Number(rawSourceIndex);
    const viewportLeft = window.scrollX;
    const viewportTop = window.scrollY;

    event.preventDefault();
    this.draggingPrecedenceIndex.set(null);

    if (
      this.savingPrecedenceList() ||
      !Number.isInteger(sourceIndex) ||
      sourceIndex === targetIndex
    ) {
      return;
    }

    const entries = this.precedenceEntries();
    if (
      sourceIndex < 0 ||
      targetIndex < 0 ||
      sourceIndex >= entries.length ||
      targetIndex >= entries.length
    ) {
      return;
    }

    const reorderedEntries = [...entries];
    const [movedEntry] = reorderedEntries.splice(sourceIndex, 1);
    reorderedEntries.splice(targetIndex, 0, movedEntry);
    this.savePrecedenceOrder(reorderedEntries);
    this.restorePrecedenceDropViewport(viewportLeft, viewportTop);
  }

  protected clearPrecedenceDrag(): void {
    this.draggingPrecedenceIndex.set(null);
  }

  private restorePrecedenceDropViewport(left: number, top: number): void {
    requestAnimationFrame(() => window.scrollTo(left, top));
  }

  private savePrecedenceOrder(entries: ContentKeyValuePair[]): void {
    const list = this.precedenceList();

    if (!list?.id) {
      this.precedenceListError.set('Precedence list cannot be saved without an id.');
      return;
    }

    const updatedList: ContentPrecedenceList = {
      branch: list.branch,
      id: list.id,
      lastModified: list.lastModified,
      lastModifiedBy: list.lastModifiedBy,
      name: list.name,
      precedence: {
        ...(list.precedence ?? {}),
        keyValuePairs: entries.map((entry) => ({
          key: entry.key ?? '',
          value: entry.value ?? ''
        }))
      },
      terminology: list.terminology,
      timestamp: list.timestamp,
      version: list.version
    };

    this.precedenceList.set(updatedList);
    this.precedenceListError.set(null);
    this.savingPrecedenceList.set(true);
    this.api
      .updatePrecedenceList(updatedList)
      .pipe(finalize(() => this.savingPrecedenceList.set(false)))
      .subscribe({
        next: () => {
          const t = this.selectedMetadataTerminology();
          if (t?.terminology && t.version) {
            this.loadPrecedenceList(t.terminology, t.version, false, true);
          }
        },
        error: () => {
          this.precedenceListError.set('Precedence list order could not be saved.');
          const t = this.selectedMetadataTerminology();
          if (t?.terminology && t.version) {
            this.loadPrecedenceList(t.terminology, t.version, true, true);
          }
        }
      });
  }

  protected removeTermType(key: string): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) {
      return;
    }
    this.removingTermTypeKey.set(key);
    this.api
      .removeTermType(key, t.terminology, t.version)
      .pipe(finalize(() => this.removingTermTypeKey.set(null)))
      .subscribe({
        next: () => this.loadMetadata(),
        error: () => this.notifications.error(`Could not remove term type "${key}".`)
      });
  }

  protected removeMetadataAttributeName(key: string): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) {
      return;
    }
    this.removingAttributeNameKey.set(key);
    this.api
      .removeAttributeName(key, t.terminology, t.version)
      .pipe(finalize(() => this.removingAttributeNameKey.set(null)))
      .subscribe({
        next: () => this.loadMetadata(),
        error: () => this.notifications.error(`Could not remove attribute name "${key}".`)
      });
  }

  protected removeMetadataRelationshipType(key: string): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) {
      return;
    }
    this.removingRelationshipTypeKey.set(key);
    this.api
      .removeRelationshipType(key, t.terminology, t.version)
      .pipe(finalize(() => this.removingRelationshipTypeKey.set(null)))
      .subscribe({
        next: () => this.loadMetadata(),
        error: () => this.notifications.error(`Could not remove relationship type "${key}".`)
      });
  }

  protected removeAdditionalRelType(key: string): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) {
      return;
    }
    this.removingAdditionalRelTypeKey.set(key);
    this.api
      .removeAdditionalRelationshipType(key, t.terminology, t.version)
      .pipe(finalize(() => this.removingAdditionalRelTypeKey.set(null)))
      .subscribe({
        next: () => this.loadMetadata(),
        error: () =>
          this.notifications.error(`Could not remove additional relationship type "${key}".`)
      });
  }

  protected setTermTypesFilter(value: string): void {
    this.termTypesFilter.set(value);
    this.termTypesPage.set(1);
  }

  protected setAttributeNamesFilter(value: string): void {
    this.attributeNamesFilter.set(value);
    this.attributeNamesPage.set(1);
  }

  protected setRelationshipTypesFilter(value: string): void {
    this.relationshipTypesFilter.set(value);
    this.relationshipTypesPage.set(1);
  }

  protected setAdditionalRelTypesFilter(value: string): void {
    this.additionalRelTypesFilter.set(value);
    this.additionalRelTypesPage.set(1);
  }

  private resetTermTypeForm(): void {
    this.metaTermTypeFormAbbreviation.set('');
    this.metaTermTypeFormExpandedForm.set('');
    this.metaTermTypeFormSuppressible.set(false);
    this.metaTermTypeFormObsolete.set(false);
    this.metaTermTypeFormHierarchicalType.set(false);
    this.metaTermTypeFormExclude.set(false);
    this.metaTermTypeFormNormExclude.set(false);
    this.metaTermTypeDetailStyle.set(null);
    this.metaTermTypeDetailUsageType.set(null);
    this.metaTermTypeDetailNameVariantType.set(null);
    this.metaTermTypeDetailCodeVariantType.set(null);
    this.metaTermTypeErrors.set([]);
  }

  private populateTermTypeForm(detail: ContentTermTypeDetail): void {
    this.metaTermTypeFormAbbreviation.set(detail.abbreviation ?? '');
    this.metaTermTypeFormExpandedForm.set(detail.expandedForm ?? '');
    this.metaTermTypeFormSuppressible.set(detail.suppressible ?? false);
    this.metaTermTypeFormObsolete.set(detail.obsolete ?? false);
    this.metaTermTypeFormHierarchicalType.set(detail.hierarchicalType ?? false);
    this.metaTermTypeFormExclude.set(detail.exclude ?? false);
    this.metaTermTypeFormNormExclude.set(detail.normExclude ?? false);
    this.metaTermTypeDetailStyle.set(detail.style ?? null);
    this.metaTermTypeDetailUsageType.set(detail.usageType ?? null);
    this.metaTermTypeDetailNameVariantType.set(detail.nameVariantType ?? null);
    this.metaTermTypeDetailCodeVariantType.set(detail.codeVariantType ?? null);
  }

  protected openAddTermType(): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    this.resetTermTypeForm();
    this.metaTermTypeDialogMode.set('addTermType');
  }

  protected openEditTermType(key: string): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    this.resetTermTypeForm();
    this.metaTermTypeDialogMode.set('editTermType');
    this.metaTermTypeLoading.set(true);
    this.api
      .getTermType(key, t.terminology, t.version)
      .pipe(finalize(() => this.metaTermTypeLoading.set(false)))
      .subscribe({
        next: (detail) => this.populateTermTypeForm(detail),
        error: () => this.metaTermTypeErrors.set(['Could not load term type details.'])
      });
  }

  protected openAddAttributeName(): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    this.resetTermTypeForm();
    this.metaTermTypeDialogMode.set('addAttributeName');
  }

  protected openEditAttributeName(key: string): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    this.resetTermTypeForm();
    this.metaTermTypeDialogMode.set('editAttributeName');
    this.metaTermTypeLoading.set(true);
    this.api
      .getAttributeName(key, t.terminology, t.version)
      .pipe(finalize(() => this.metaTermTypeLoading.set(false)))
      .subscribe({
        next: (detail) => this.populateTermTypeForm(detail),
        error: () => this.metaTermTypeErrors.set(['Could not load attribute name details.'])
      });
  }

  protected closeMetaTermTypeDialog(): void {
    this.metaTermTypeDialogMode.set(null);
  }

  protected submitMetaTermTypeDialog(): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    const mode = this.metaTermTypeDialogMode();
    if (!mode) return;

    const obj: ContentTermTypeDetail = {
      abbreviation: this.metaTermTypeFormAbbreviation().trim(),
      expandedForm: this.metaTermTypeFormExpandedForm().trim(),
      terminology: t.terminology,
      version: t.version
    };
    if (mode === 'editTermType' || mode === 'addTermType') {
      obj.suppressible = this.metaTermTypeFormSuppressible();
      obj.obsolete = this.metaTermTypeFormObsolete();
      obj.hierarchicalType = this.metaTermTypeFormHierarchicalType();
      obj.exclude = this.metaTermTypeFormExclude();
      obj.normExclude = this.metaTermTypeFormNormExclude();
    }

    this.metaTermTypeSubmitting.set(true);
    this.metaTermTypeErrors.set([]);

    let call$: Observable<unknown>;
    if (mode === 'addTermType') call$ = this.api.addTermType(obj);
    else if (mode === 'editTermType') call$ = this.api.updateTermType(obj);
    else if (mode === 'addAttributeName') call$ = this.api.addAttributeName(obj);
    else call$ = this.api.updateAttributeName(obj);

    call$.pipe(finalize(() => this.metaTermTypeSubmitting.set(false))).subscribe({
      next: () => {
        this.metaTermTypeDialogMode.set(null);
        this.loadMetadata();
      },
      error: () => this.metaTermTypeErrors.set(['The operation could not be completed.'])
    });
  }

  private resetRelTypeForm(): void {
    this.metaRelTypeFormAbbreviation.set('');
    this.metaRelTypeFormExpandedForm.set('');
    this.metaRelTypeInverseFormAbbreviation.set('');
    this.metaRelTypeInverseFormExpandedForm.set('');
    this.metaRelTypeErrors.set([]);
  }

  protected openAddRelationshipType(): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    this.resetRelTypeForm();
    this.metaRelTypeDialogMode.set('addRelType');
  }

  protected openEditRelationshipType(key: string): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    this.resetRelTypeForm();
    this.metaRelTypeDialogMode.set('editRelType');
    this.metaRelTypeLoading.set(true);
    this.api
      .getRelationshipType(key, t.terminology, t.version)
      .pipe(finalize(() => this.metaRelTypeLoading.set(false)))
      .subscribe({
        next: (detail) => {
          this.metaRelTypeFormAbbreviation.set(detail.abbreviation ?? '');
          this.metaRelTypeFormExpandedForm.set(detail.expandedForm ?? '');
          this.metaRelTypeInverseFormAbbreviation.set(detail.inverseAbbreviation ?? '');
        },
        error: () => this.metaRelTypeErrors.set(['Could not load relationship type details.'])
      });
  }

  protected openAddAdditionalRelType(): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    this.resetRelTypeForm();
    this.metaRelTypeDialogMode.set('addAddRelType');
  }

  protected openEditAdditionalRelType(key: string): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    this.resetRelTypeForm();
    this.metaRelTypeDialogMode.set('editAddRelType');
    this.metaRelTypeLoading.set(true);
    this.api
      .getAdditionalRelationshipType(key, t.terminology, t.version)
      .pipe(finalize(() => this.metaRelTypeLoading.set(false)))
      .subscribe({
        next: (detail) => {
          this.metaRelTypeFormAbbreviation.set(detail.abbreviation ?? '');
          this.metaRelTypeFormExpandedForm.set(detail.expandedForm ?? '');
          this.metaRelTypeInverseFormAbbreviation.set(detail.inverseAbbreviation ?? '');
        },
        error: () => this.metaRelTypeErrors.set(['Could not load additional relationship type details.'])
      });
  }

  protected closeMetaRelTypeDialog(): void {
    this.metaRelTypeDialogMode.set(null);
  }

  protected submitMetaRelTypeDialog(): void {
    const t = this.selectedMetadataTerminology();
    if (!t?.terminology || !t?.version) return;
    const mode = this.metaRelTypeDialogMode();
    if (!mode) return;

    const obj: ContentRelationshipTypeDetail = {
      abbreviation: this.metaRelTypeFormAbbreviation().trim(),
      expandedForm: this.metaRelTypeFormExpandedForm().trim(),
      terminology: t.terminology,
      version: t.version
    };

    this.metaRelTypeSubmitting.set(true);
    this.metaRelTypeErrors.set([]);

    let call$: Observable<unknown>;
    if (mode === 'addRelType') {
      const inverse: ContentRelationshipTypeDetail = {
        abbreviation: this.metaRelTypeInverseFormAbbreviation().trim(),
        expandedForm: this.metaRelTypeInverseFormExpandedForm().trim(),
        terminology: t.terminology,
        version: t.version
      };
      call$ = this.api.addRelationshipType({ types: [obj, inverse] });
    } else if (mode === 'editRelType') {
      call$ = this.api.updateRelationshipType(obj);
    } else if (mode === 'addAddRelType') {
      const inverse: ContentRelationshipTypeDetail = {
        abbreviation: this.metaRelTypeInverseFormAbbreviation().trim(),
        expandedForm: this.metaRelTypeInverseFormExpandedForm().trim(),
        terminology: t.terminology,
        version: t.version
      };
      call$ = this.api.addAdditionalRelationshipType({ types: [obj, inverse] });
    } else {
      call$ = this.api.updateAdditionalRelationshipType(obj);
    }

    call$.pipe(finalize(() => this.metaRelTypeSubmitting.set(false))).subscribe({
      next: () => {
        this.metaRelTypeDialogMode.set(null);
        this.loadMetadata();
      },
      error: () => this.metaRelTypeErrors.set(['The operation could not be completed.'])
    });
  }

  // Concept list

  protected addConceptToList(concept: ContentComponentDetail): void {
    const list = this.conceptList();
    if (list.some((c) => c.id === concept.id)) return;
    const updated = [...list, concept].sort((a, b) => (a.id ?? 0) - (b.id ?? 0));
    this.conceptList.set(updated);
    if (this.pendingEditConceptId && concept.id === this.pendingEditConceptId) {
      this.pendingEditConceptId = null;
      this.selectConceptFromList(concept);
    } else if (updated.length === 1 && !this.pendingEditConceptId) {
      this.selectConceptFromList(updated[0]);
    }
  }

  protected removeConceptFromList(concept: ContentComponentDetail): void {
    const updated = this.conceptList().filter((c) => c.id !== concept.id);
    this.conceptList.set(updated);
    if (this.selectedComponent()?.id === concept.id) {
      if (updated.length === 1) {
        this.selectConceptFromList(updated[0]);
      } else {
        this.selectedComponent.set(null);
        this.saveUserPreferenceProperties({ editConcept: '' });
      }
    } else if (updated.length === 1 && !this.selectedComponent()) {
      this.selectConceptFromList(updated[0]);
    }
  }

  protected reloadConceptInList(concept: ContentComponentDetail): void {
    const projectId = this.projectId();
    if (!concept.id || !projectId) return;
    this.api.getComponentById('concept', concept.id, projectId).subscribe({
      next: (updated) => {
        if (!updated) return;
        this.conceptList.update((list) =>
          list.map((c) => (c.id === concept.id ? updated : c)).sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
        );
        if (this.selectedComponent()?.id === concept.id) {
          this.selectedComponent.set(updated);
        }
      },
      error: () => {}
    });
  }

  private refreshConceptListAfterMerge(
    fromConceptId: number,
    toConceptId: number | null | undefined
  ): void {
    this.conceptList.update((list) =>
      list.filter((concept) => concept.id !== fromConceptId)
    );
    if (this.selectedComponent()?.id === fromConceptId) {
      this.selectedComponent.set(null);
      this.selectedResult.set(null);
      this.saveUserPreferenceProperties({ editConcept: '' });
    }

    const projectId = this.projectId();
    if (!projectId || !toConceptId) {
      return;
    }

    this.api.getComponentById('concept', toConceptId, projectId).subscribe({
      next: (concept) => {
        if (!concept) {
          return;
        }
        this.conceptList.update((list) =>
          [
            ...list.filter((item) => item.id !== fromConceptId && item.id !== toConceptId),
            concept
          ].sort((a, b) => (a.id ?? 0) - (b.id ?? 0))
        );
        this.selectConceptFromList(concept);
      },
      error: () => {
        const existingTarget = this.conceptList().find(
          (concept) => concept.id === toConceptId
        );
        if (existingTarget) {
          this.selectConceptFromList(existingTarget);
          return;
        }
        this.notifications.error('Merged concept could not be reloaded.');
      }
    });
  }

  protected selectConceptFromList(concept: ContentComponentDetail): void {
    this.selectedComponent.set(concept);
    this.selectedResult.set(null);
    if (concept.id) {
      this.saveUserPreferenceProperties({ editConcept: concept.id });
    }
  }

  protected lookupConceptById(): void {
    const rawId = String(this.finderLookupId()).trim();
    if (!rawId.match(/^[1-9]\d*$/)) return;
    const id = Number(rawId);
    const projectId = this.projectId();
    if (!projectId) return;
    this.loadingFinderLookup.set(true);
    this.api.getComponentById('concept', id, projectId).pipe(
      finalize(() => this.loadingFinderLookup.set(false))
    ).subscribe({
      next: (concept) => {
        if (concept) { this.addConceptToList(concept); this.finderLookupId.set(''); }
        else this.notifications.error('Concept not found.');
      },
      error: () => this.notifications.error('Concept not found.')
    });
  }

  protected openFinderDialog(mode: FinderDialogMode = 'concept-list'): void {
    this.finderDialogMode.set(mode);
    this.finderQuery.set('');
    this.finderResults.set([]);
    this.finderResultsTotal.set(0);
    this.finderResultsPage.set(1);
    this.finderSelectedResult.set(null);
    this.finderPreviewConcept.set(null);
    this.finderDialogOpen.set(true);
  }

  protected selectFinderResult(result: ContentSearchResult): void {
    this.finderSelectedResult.set(result);
    const projectId = this.projectId();
    if (!result.id || !projectId) return;
    this.api.getComponentById('concept', result.id, projectId).subscribe({
      next: (concept) => this.finderPreviewConcept.set(concept)
    });
  }

  protected closeFinderDialog(): void {
    this.finderDialogOpen.set(false);
  }

  protected runFinderSearch(): void {
    const q = this.finderQuery().trim();
    if (!q) return;
    const project = this.availableProjects().find((p) => p.id === this.projectId());
    const terminology =
      project?.terminology || this.terminology() || this.currentTerminologies()[0]?.terminology;
    const version =
      project?.version || this.version() || this.currentTerminologies()[0]?.version;
    if (!terminology) {
      this.notifications.error('No terminology available — select a project first.');
      return;
    }
    this.loadingFinderResults.set(true);
    const pfs = buildContentSearchPfs(
      this.finderResultsPage(),
      this.finderResultsPageSize,
      null,
      true,
      'CONCEPT'
    );
    this.api.findComponents('CONCEPT', terminology, version ?? '', q, pfs)
      .pipe(finalize(() => this.loadingFinderResults.set(false))).subscribe({
        next: (resp) => {
          this.finderResults.set(resp.items ?? []);
          this.finderResultsTotal.set(resp.totalCount ?? 0);
        },
        error: () => this.notifications.error('Finder search failed.')
      });
  }

  protected openLinkedConceptDialog(info: LinkedConceptInfo): void {
    const params = new URLSearchParams();
    const projectId = this.projectId();
    if (projectId) params.set('projectId', String(projectId));
    if (info.tab) params.set('tab', info.tab);

    let url: string;
    let windowName: string;
    if (info.terminologyId && info.terminology && info.version) {
      url = memeAppRouteUrl(
        `/concept-report/${encodeURIComponent(info.terminology)}/${encodeURIComponent(info.version)}/${encodeURIComponent(info.terminologyId)}`,
        params
      );
      windowName = `concept_${info.terminologyId}`;
    } else if (info.id) {
      params.set('id', String(info.id));
      url = memeAppRouteUrl('/concept-report', params);
      windowName = `concept_id_${info.id}`;
    } else {
      return;
    }
    window.open(url, windowName, 'width=700,height=700,scrollbars=yes');
  }

  protected handleReportClick(event: MouseEvent): void {
    if (!(event.target instanceof HTMLElement)) {
      return;
    }

    const anchor = event.target.closest(
      'a[data-concept-id]'
    ) as HTMLAnchorElement | null;
    if (!anchor) {
      return;
    }

    event.preventDefault();
    const conceptId = Number(anchor.getAttribute('data-concept-id'));
    if (!Number.isFinite(conceptId)) {
      return;
    }

    const params = new URLSearchParams();
    const projectId = this.projectId();
    if (projectId) params.set('projectId', String(projectId));
    params.set('tab', 'Report');
    params.set('id', String(conceptId));
    window.open(
      memeAppRouteUrl('/concept-report', params),
      `concept_id_${conceptId}`,
      'width=700,height=700,scrollbars=yes'
    );
  }

  protected addFinderResultToList(): void {
    const result = this.finderSelectedResult();
    if (!result) return;
    const projectId = this.projectId();
    if (!projectId || !result.id) return;
    this.api.getComponentById('concept', result.id, projectId).subscribe({
      next: (concept) => {
        if (!concept) {
          return;
        }
        if (this.finderDialogMode() === 'merge') {
          const target = this.toMergeTargetOption(concept);
          this.addMergeTargetCandidate(target);
          this.selectMergeTarget(target);
        } else {
          this.addConceptToList(concept);
        }
        this.closeFinderDialog();
      },
      error: () => this.notifications.error('Could not load concept.')
    });
  }

  protected addSearchResultToList(result: ContentSearchResult): void {
    const projectId = this.projectId();
    if (!result.id || !projectId) return;
    this.api.getComponentById('concept', result.id, projectId).subscribe({
      next: (concept) => { if (concept) this.addConceptToList(concept); },
      error: () => this.notifications.error('Could not load concept.')
    });
  }

  private addMergeTargetCandidate(target: MergeTargetOption): void {
    const id = target.id;
    if (!id || id === this.selectedComponent()?.id) {
      return;
    }

    this.mergeTargetCandidates.update((targets) => {
      const next = targets.filter((existing) => existing.id !== id);
      return [...next, target];
    });
  }

  private selectDefaultMergeTarget(): void {
    const selectedId = this.selectedComponent()?.id;
    const target = this.conceptList().find(
      (concept) => Boolean(concept.id) && concept.id !== selectedId
    );

    if (target) {
      this.selectMergeTarget(this.toMergeTargetOption(target));
    }
  }

  private toMergeTargetOption(concept: ContentComponentDetail): MergeTargetOption {
    return {
      id: concept.id,
      lastModified: concept.lastModified,
      name: concept.name,
      obsolete: concept.obsolete,
      publishable: concept.publishable,
      published: concept.published,
      semanticTypes: concept.semanticTypes,
      suppressible: concept.suppressible,
      terminology: concept.terminology,
      terminologyId: concept.terminologyId,
      type: concept.type,
      value: concept.name,
      version: concept.version,
      workflowStatus: concept.workflowStatus
    };
  }

  protected nextRecord(): void {
    const worklist = this.selectedWorklist();
    const record = this.selectedRecord();
    const records = this.records();
    if (!worklist || !record || !records.length) return;
    const idx = records.findIndex((r) => r.id === record.id);
    if (idx >= 0 && idx < records.length - 1) {
      this.selectRecord(records[idx + 1]);
    }
  }

  protected approveAndNext(): void {
    const list = this.conceptList();
    if (!list.length) { this.nextRecord(); return; }
    let remaining = list.length;
    for (const concept of list) {
      if (!concept.id) { remaining--; continue; }
      this.approveSelectedConcept(false);
      remaining--;
      if (remaining === 0) this.nextRecord();
    }
  }

  // Project / Role selection

  protected loadProjects(): void {
    const user = this.auth.currentUser();
    const projectIds = Object.keys(user.projectRoleMap ?? {}).map(Number).filter(Boolean);
    this.operationsApi.findProjectsByIds(projectIds).subscribe({
      next: (projects) => this.availableProjects.set(projects),
      error: () => {}
    });
  }

  protected selectProject(idStr: string): void {
    const user = this.auth.currentUser();
    const newPrefs = {
      ...user.userPreferences,
      lastProjectId: Number(idStr),
      lastProjectRole: null
    };
    this.operationsApi.updateUserPreferences(newPrefs).subscribe({
      next: (saved) => {
        this.auth.updateCurrentUserPreferences(saved ?? newPrefs);
        this.loadProjectContext();
        this.refreshWorkflowForContextChange();
      },
      error: () => this.notifications.error('Could not switch project.')
    });
  }

  protected selectRole(role: string): void {
    const user = this.auth.currentUser();
    const newPrefs = { ...user.userPreferences, lastProjectRole: role };
    this.operationsApi.updateUserPreferences(newPrefs).subscribe({
      next: (saved) => {
        this.auth.updateCurrentUserPreferences(saved ?? newPrefs);
        this.refreshWorkflowForContextChange();
      },
      error: () => this.notifications.error('Could not switch role.')
    });
  }

  protected setProjectEditingEnabled(enabled: boolean): void {
    const project = this.selectedProject();

    if (!project?.id) {
      return;
    }

    const updatedProject: OperationalProject = {
      ...project,
      editingEnabled: enabled
    };

    this.updatingProjectEditing.set(true);
    this.operationsApi
      .updateProject(updatedProject)
      .pipe(finalize(() => this.updatingProjectEditing.set(false)))
      .subscribe({
        next: () => {
          this.availableProjects.update((projects) =>
            projects.map((item) =>
              item.id === project.id ? { ...item, editingEnabled: enabled } : item
            )
          );
          this.projectEditingEnabled.set(enabled);
        },
        error: () => this.notifications.error('Could not update project editing state.')
      });
  }

  protected onEditAccordionToggle(group: EditAccordionGroup, event: Event): void {
    const isOpen = (event.target as HTMLDetailsElement).open;

    if (group === 'worklists') {
      this.worklistsGroupOpen.set(isOpen);
    } else if (group === 'concepts') {
      this.conceptsGroupOpen.set(isOpen);
    } else {
      this.metadataGroupOpen.set(isOpen);
    }

    this.saveEditAccordionState();
  }

  private restoreEditPreferences(): void {
    const properties = this.auth.currentUser().userPreferences?.properties ?? {};
    const savedMode = this.matchingWorklistMode(properties['worklistModeTab']);

    if (savedMode) {
      this.worklistMode.set(savedMode);
    }

    this.pendingEditWorklistId = this.parsePreferenceId(properties['editWorklist']);
    this.pendingEditRecordId = this.parsePreferenceId(properties['editRecord']);
    this.pendingEditConceptId = this.parsePreferenceId(properties['editConcept']);
    this.reportPanelTab.set(this.matchingReportPanelTab(properties['reportModeTab']));
    this.applyStoredWorklistPaging(properties['editWorklistPaging']);

    const groups = this.parseStoredGroups(properties['editGroups']);

    if (!groups.length) {
      return;
    }

    this.applyStoredGroup(groups, 0, 'Worklists/Clusters', this.worklistsGroupOpen);
    this.applyStoredGroup(groups, 1, 'Concepts/Reports', this.conceptsGroupOpen);
    this.applyStoredGroup(groups, 2, 'Metadata', this.metadataGroupOpen);
  }

  private applyStoredWorklistPaging(raw: unknown): void {
    const paging = this.parseStoredPaging(raw);

    if (!paging) {
      return;
    }

    const page = this.positiveNumber(paging.page);
    const sortField = paging.sortField === 'name' || paging.sortField === 'lastModified'
      ? paging.sortField
      : null;

    if (page) {
      this.worklistPage.set(page);
    }
    if (sortField) {
      this.worklistSortField.set(sortField);
    }
    if (typeof paging.sortAscending === 'boolean') {
      this.worklistSortAsc.set(paging.sortAscending);
    }
    if (typeof paging.filter === 'string') {
      this.worklistFilter.set(paging.filter);
    }
  }

  private applyStoredRecordPaging(raw: unknown): void {
    const paging = this.parseStoredPaging(raw);

    if (!paging) {
      return;
    }

    const page = this.positiveNumber(paging.page);
    const pageSize = this.positiveNumber(paging.pageSize);

    if (page) {
      this.recordsPage.set(page);
    }
    if (pageSize) {
      this.recordsPageSize.set(pageSize);
    }
    if (typeof paging.filter === 'string') {
      this.recordsFilter.set(paging.filter);
    }
    if (typeof paging.typeFilter === 'string') {
      this.recordsTypeFilter.set(paging.typeFilter);
    }
  }

  private saveEditAccordionState(): void {
    this.saveUserPreferenceProperties({
      editGroups: JSON.stringify([
        { open: this.worklistsGroupOpen(), title: 'Worklists/Clusters' },
        { open: this.conceptsGroupOpen(), title: 'Concepts/Reports' },
        { open: this.metadataGroupOpen(), title: 'Metadata' }
      ])
    });
  }

  private matchingWorklistMode(value: unknown): WorklistMode | null {
    return (
      ['Available', 'Assigned', 'Done', 'Checklists'] as WorklistMode[]
    ).find((mode) => mode === value) ?? null;
  }

  protected onReportPanelTabChanged(tab: ReportPanelTab): void {
    this.reportPanelTab.set(tab);
    this.saveUserPreferenceProperties({ reportModeTab: this.reportModePreference(tab) });
  }

  private matchingReportPanelTab(value: unknown): ReportPanelTab {
    if (value === 'Static' || value === 'Report') {
      return 'Report';
    }
    if (value === 'Interactive') {
      return 'Interactive';
    }
    if (value === 'Action' || value === 'Actions') {
      return 'Actions';
    }

    return 'Report';
  }

  private reportModePreference(tab: ReportPanelTab): 'Static' | 'Interactive' | 'Action' {
    if (tab === 'Interactive') {
      return 'Interactive';
    }
    if (tab === 'Actions') {
      return 'Action';
    }

    return 'Static';
  }

  private parseStoredGroups(raw: unknown): Array<{ open?: unknown; title?: unknown }> {
    if (Array.isArray(raw)) {
      return raw as Array<{ open?: unknown; title?: unknown }>;
    }

    if (typeof raw !== 'string' || !raw) {
      return [];
    }

    try {
      const parsed = JSON.parse(raw) as unknown;

      return Array.isArray(parsed)
        ? (parsed as Array<{ open?: unknown; title?: unknown }>)
        : [];
    } catch {
      return [];
    }
  }

  private parseStoredPaging(raw: unknown): EditPagingPreference | null {
    if (!raw) {
      return null;
    }

    if (typeof raw === 'object' && !Array.isArray(raw)) {
      return raw as EditPagingPreference;
    }

    if (typeof raw !== 'string') {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as unknown;

      return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
        ? (parsed as EditPagingPreference)
        : null;
    } catch {
      return null;
    }
  }

  private applyStoredGroup(
    groups: Array<{ open?: unknown; title?: unknown }>,
    index: number,
    title: string,
    target: { set(value: boolean): void }
  ): void {
    const byTitle = groups.find((storedGroup) => storedGroup.title === title);
    const group = byTitle ?? groups[index];

    if (typeof group?.open === 'boolean') {
      target.set(group.open);
    }
  }

  private saveUserPreferenceProperties(properties: Record<string, unknown>): void {
    if (this.auth.isGuest()) {
      return;
    }

    const user = this.auth.currentUser();
    const preferences = user.userPreferences ?? { properties: {} };
    const nextPreferences = {
      ...preferences,
      properties: {
        ...(preferences.properties ?? {}),
        ...properties
      }
    };

    this.operationsApi.updateUserPreferences(nextPreferences).subscribe({
      next: (saved) => this.auth.updateCurrentUserPreferences(saved ?? nextPreferences),
      error: () => {}
    });
  }

  private parsePreferenceId(value: unknown): number | null {
    if (typeof value === 'number') {
      return Number.isInteger(value) && value > 0 ? value : null;
    }

    if (typeof value !== 'string') {
      return null;
    }

    return this.parsePositiveInteger(value);
  }

  private positiveNumber(value: unknown): number | null {
    const parsed = Number(value);

    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }

  // Worklist / Clusters

  private worklistPfs(): import('./content-edit.models').ContentPfsParameter {
    return {
      ascending: this.worklistSortAsc(),
      maxResults: this.worklistPageSize,
      queryRestriction: buildWorkflowListFilterQuery(this.worklistFilter()),
      sortField: this.worklistSortField(),
      startIndex: (this.worklistPage() - 1) * this.worklistPageSize
    };
  }

  private recordsPfs(): import('./content-edit.models').ContentPfsParameter {
    const parts: string[] = [];
    const typeFilter = this.recordsTypeFilter();
    if (typeFilter === 'N') parts.push('workflowStatus:N*');
    else if (typeFilter === 'R') parts.push('workflowStatus:R*');
    const textFilter = this.recordsFilter().trim();
    if (textFilter) parts.push(textFilter);
    return {
      ascending: true,
      maxResults: this.recordsPageSize(),
      queryRestriction: parts.length ? parts.join(' AND ') : undefined,
      sortField: 'clusterId',
      startIndex: (this.recordsPage() - 1) * this.recordsPageSize()
    };
  }

  private worklistUser(): { projectId: number; userName: string; role: string } | null {
    const projectId = this.projectId();
    const userName = this.auth.currentUser().userName;
    const role = this.projectRole();
    if (!projectId || !userName || !role || role === 'n/a') return null;
    return { projectId, userName, role };
  }

  private isCurrentWorklistUser(
    ctx: { projectId: number; userName: string; role: string }
  ): boolean {
    const current = this.worklistUser();

    return !!current &&
      current.projectId === ctx.projectId &&
      current.userName === ctx.userName &&
      current.role === ctx.role;
  }

  private refreshWorkflowForContextChange(): void {
    this.worklistPage.set(1);
    this.worklistFilter.set('');
    this.pendingEditWorklistId = null;
    this.pendingEditRecordId = null;
    this.pendingEditConceptId = null;
    this.selectedWorklist.set(null);
    this.records.set([]);
    this.recordsTotalCount.set(0);
    this.selectedRecord.set(null);
    this.recordsPage.set(1);
    this.recordsTypeFilter.set('');
    this.recordsFilter.set('');
    this.availableCt.set(0);
    this.assignedCt.set(0);
    this.doneCt.set(0);
    this.checklistCt.set(0);
    this.worklists.set([]);
    this.worklistsTotalCount.set(0);

    if (this.projectRole() !== 'ADMINISTRATOR') {
      this.loadWorklists();
      this.loadTabCounts();
    }
  }

  protected loadWorklists(): void {
    const ctx = this.worklistUser();
    if (!ctx) return;
    const mode = this.worklistMode();
    this.loadingWorklists.set(true);
    const pfs = this.worklistPfs();

    const source$: Observable<{ worklists?: WorkflowWorklist[] | null; checklists?: WorkflowWorklist[] | null; objects?: WorkflowWorklist[] | null; totalCount?: number | null }> =
      mode === 'Available'
        ? this.workflowApi.findAvailableWorklists(ctx.projectId, ctx.userName, ctx.role, pfs)
        : mode === 'Assigned'
          ? this.workflowApi.findAssignedWorklists(ctx.projectId, ctx.userName, ctx.role, pfs)
          : mode === 'Done'
            ? this.workflowApi.findDoneWorklists(ctx.projectId, ctx.userName, ctx.role, pfs)
            : this.workflowApi.findChecklists(ctx.projectId, '', pfs);

    source$.pipe(finalize(() => this.loadingWorklists.set(false))).subscribe({
      next: (resp) => {
        if (!this.isCurrentWorklistUser(ctx) || this.worklistMode() !== mode) {
          return;
        }
        const items: WorkflowWorklist[] = resp.worklists ?? resp.checklists ?? resp.objects ?? [];
        const total = resp.totalCount ?? 0;
        this.worklists.set(items);
        this.worklistsTotalCount.set(total);
        // update the count for the active tab
        if (mode === 'Available') this.availableCt.set(total);
        else if (mode === 'Assigned') this.assignedCt.set(total);
        else if (mode === 'Done') this.doneCt.set(total);
        else this.checklistCt.set(total);
        this.restoreSelectedWorklist(items);
      },
      error: () => {}
    });
  }

  private loadTabCounts(): void {
    const ctx = this.worklistUser();
    if (!ctx) return;
    const minPfs: import('./content-edit.models').ContentPfsParameter = {
      ascending: false,
      maxResults: 1,
      startIndex: 0,
      sortField: 'lastModified'
    };
    this.workflowApi.findAvailableWorklists(ctx.projectId, ctx.userName, ctx.role, minPfs)
      .subscribe({
        next: (r) => {
          if (this.isCurrentWorklistUser(ctx)) {
            this.availableCt.set(r.totalCount ?? 0);
          }
        },
        error: () => {}
      });
    this.workflowApi.findAssignedWorklists(ctx.projectId, ctx.userName, ctx.role, minPfs)
      .subscribe({
        next: (r) => {
          if (this.isCurrentWorklistUser(ctx)) {
            this.assignedCt.set(r.totalCount ?? 0);
          }
        },
        error: () => {}
      });
    this.workflowApi.findDoneWorklists(ctx.projectId, ctx.userName, ctx.role, minPfs)
      .subscribe({
        next: (r) => {
          if (this.isCurrentWorklistUser(ctx)) {
            this.doneCt.set(r.totalCount ?? 0);
          }
        },
        error: () => {}
      });
    this.workflowApi.findChecklists(ctx.projectId, '', minPfs)
      .subscribe({
        next: (r) => {
          if (this.isCurrentWorklistUser(ctx)) {
            this.checklistCt.set(r.totalCount ?? 0);
          }
        },
        error: () => {}
      });
  }

  protected setWorklistMode(mode: WorklistMode): void {
    this.worklistMode.set(mode);
    this.worklistPage.set(1);
    this.worklistFilter.set('');
    this.pendingEditWorklistId = null;
    this.pendingEditRecordId = null;
    this.pendingEditConceptId = null;
    this.selectedWorklist.set(null);
    this.records.set([]);
    this.recordsTotalCount.set(0);
    this.selectedRecord.set(null);
    this.saveUserPreferenceProperties({ worklistModeTab: mode });
    this.loadWorklists();
  }

  protected setWorklistFilter(value: string): void {
    this.worklistFilter.set(value);
    this.worklistPage.set(1);
    this.loadWorklists();
  }

  protected setWorklistSortField(field: 'name' | 'lastModified'): void {
    if (this.worklistSortField() === field) {
      this.worklistSortAsc.update((v) => !v);
    } else {
      this.worklistSortField.set(field);
      this.worklistSortAsc.set(false);
    }
    this.worklistPage.set(1);
    this.loadWorklists();
  }

  protected selectWorklist(
    worklist: WorkflowWorklist,
    recoverPreferences = false
  ): void {
    this.selectedWorklist.set(worklist);
    if (worklist.name?.trim()) {
      this.editActivityId.set(worklist.name);
    }
    this.pendingEditWorklistId = null;
    if (recoverPreferences) {
      this.applyStoredRecordPaging(
        this.auth.currentUser().userPreferences?.properties?.['editRecordPaging']
      );
    } else {
      this.pendingEditRecordId = null;
      this.pendingEditConceptId = null;
      this.recordsPage.set(1);
      this.recordsTypeFilter.set('');
      this.recordsFilter.set('');
    }
    this.selectedRecord.set(null);
    this.records.set([]);
    this.recordsTotalCount.set(0);
    this.saveUserPreferenceProperties({
      editWorklist: worklist.id ?? '',
      editWorklistPaging: JSON.stringify(this.currentWorklistPagingPreference())
    });
    this.loadRecords();
  }

  protected loadRecords(): void {
    const ctx = this.worklistUser();
    const worklist = this.selectedWorklist();
    if (!ctx || !worklist?.id) return;
    const mode = this.worklistMode();
    this.loadingRecords.set(true);
    const pfs = this.recordsPfs();

    const source$ = mode === 'Checklists'
      ? this.workflowApi.findTrackingRecordsForChecklist(ctx.projectId, worklist.id, pfs)
      : this.workflowApi.findTrackingRecordsForWorklist(ctx.projectId, worklist.id, pfs);

    source$.pipe(finalize(() => this.loadingRecords.set(false))).subscribe({
      next: (resp) => {
        const records = resp.records ?? resp.objects ?? [];
        this.records.set(records);
        this.recordsTotalCount.set(resp.totalCount ?? 0);
        this.restoreSelectedRecord(records);
      },
      error: () => {}
    });
  }

  protected selectRecord(
    record: WorkflowTrackingRecord,
    recoverPreferences = false
  ): void {
    this.selectedRecord.set(record);
    this.conceptList.set([]);
    this.selectedComponent.set(null);
    this.pendingEditRecordId = null;
    if (!recoverPreferences) {
      this.pendingEditConceptId = null;
    } else if (
      this.pendingEditConceptId &&
      !(record.concepts ?? []).some((concept) => concept.id === this.pendingEditConceptId)
    ) {
      this.pendingEditConceptId = null;
    }
    this.saveUserPreferenceProperties({
      editRecord: record.id ?? '',
      editRecordPaging: JSON.stringify(this.currentRecordPagingPreference())
    });

    const projectId = this.projectId();
    if (!projectId || !record.concepts?.length) return;

    for (const wfConcept of record.concepts) {
      if (!wfConcept.id) continue;
      this.api.getComponentById('concept', wfConcept.id, projectId).subscribe({
        next: (concept) => { if (concept) this.addConceptToList(concept); },
        error: () => {}
      });
    }
  }

  private restoreSelectedWorklist(worklists: WorkflowWorklist[]): void {
    const pendingId = this.pendingEditWorklistId;

    if (!pendingId || this.selectedWorklist()) {
      return;
    }

    const worklist = worklists.find((item) => item.id === pendingId);

    if (worklist) {
      this.selectWorklist(worklist, true);
    }
  }

  private restoreSelectedRecord(records: WorkflowTrackingRecord[]): void {
    const pendingId = this.pendingEditRecordId;

    if (!pendingId || this.selectedRecord() || this.selectedResult() || this.selectedComponent()) {
      return;
    }

    const record = records.find((item) => item.id === pendingId);

    if (record) {
      this.selectRecord(record, true);
    }
  }

  private currentWorklistPagingPreference(): EditPagingPreference {
    return {
      filter: this.worklistFilter(),
      page: this.worklistPage(),
      pageSize: this.worklistPageSize,
      sortAscending: this.worklistSortAsc(),
      sortField: this.worklistSortField()
    };
  }

  private currentRecordPagingPreference(): EditPagingPreference {
    return {
      filter: this.recordsFilter(),
      page: this.recordsPage(),
      pageSize: this.recordsPageSize(),
      sortAscending: true,
      sortField: 'clusterId',
      typeFilter: this.recordsTypeFilter()
    };
  }

  protected setRecordsTypeFilter(value: string): void {
    this.recordsTypeFilter.set(value);
    this.recordsPage.set(1);
    this.loadRecords();
  }

  protected setRecordsFilter(value: string): void {
    this.recordsFilter.set(value);
    this.recordsPage.set(1);
    this.loadRecords();
  }

  protected setRecordsPage(page: number): void {
    this.recordsPage.set(page);
    this.loadRecords();
  }

  protected getWorkflowState(worklist: WorkflowWorklist): string {
    const history = worklist.workflowStateHistory;
    if (!history) return '';
    let maxTs = 0;
    let maxState = '';
    for (const [state, ts] of Object.entries(history)) {
      if (ts > maxTs) { maxTs = ts; maxState = state; }
    }
    return maxState;
  }

  protected assignWorklistToSelf(worklist: WorkflowWorklist, event: Event): void {
    event.stopPropagation();
    const ctx = this.worklistUser();
    if (!ctx || !worklist.id) return;
    const role = worklist.workflowStatus === 'NEW' ? 'AUTHOR' : ctx.role;
    this.workflowApi.performWorkflowAction(ctx.projectId, worklist.id, ctx.userName, role, 'ASSIGN')
      .subscribe({
        next: () => { this.loadWorklists(); this.loadTabCounts(); },
        error: () => this.notifications.error('Could not claim worklist.')
      });
  }

  protected unassignWorklist(worklist: WorkflowWorklist, event: Event): void {
    event.stopPropagation();
    const ctx = this.worklistUser();
    if (!ctx || !worklist.id) return;
    const role = (worklist.reviewers?.length ?? 0) === 0 ? 'AUTHOR' : ctx.role;
    this.workflowApi.performWorkflowAction(ctx.projectId, worklist.id, ctx.userName, role, 'UNASSIGN')
      .subscribe({
        next: () => { this.loadWorklists(); this.loadTabCounts(); },
        error: () => this.notifications.error('Could not unassign worklist.')
      });
  }

  protected reassignWorklist(worklist: WorkflowWorklist, event: Event): void {
    event.stopPropagation();
    const ctx = this.worklistUser();
    if (!ctx || !worklist.id) return;
    const role = (worklist.reviewers?.length ?? 0) === 0 ? 'AUTHOR' : ctx.role;
    this.workflowApi.performWorkflowAction(ctx.projectId, worklist.id, ctx.userName, role, 'REASSIGN')
      .subscribe({
        next: () => { this.loadWorklists(); this.loadTabCounts(); },
        error: () => this.notifications.error('Could not reassign worklist.')
      });
  }

  protected stampWorklist(worklist: WorkflowWorklist, event: Event): void {
    event.stopPropagation();
    const ctx = this.worklistUser();
    if (!ctx || !worklist.id) return;
    this.workflowApi.performWorkflowAction(ctx.projectId, worklist.id, ctx.userName, ctx.role, 'APPROVE')
      .subscribe({
        next: () => { this.loadWorklists(); this.loadTabCounts(); },
        error: () => this.notifications.error('Could not stamp worklist.')
      });
  }

  protected finishWorklist(worklist: WorkflowWorklist, event: Event): void {
    event.stopPropagation();
    const ctx = this.worklistUser();
    if (!ctx || !worklist.id) return;
    this.workflowApi.performWorkflowAction(ctx.projectId, worklist.id, ctx.userName, ctx.role, 'FINISH')
      .subscribe({
        next: () => { this.loadWorklists(); this.loadTabCounts(); },
        error: () => this.notifications.error('Could not finish worklist.')
      });
  }

  protected deleteWorklist(worklist: WorkflowWorklist, event: Event): void {
    event.stopPropagation();
    const ctx = this.worklistUser();
    if (!ctx || !worklist.id) return;
    const mode = this.worklistMode();
    const remove$ = mode === 'Checklists'
      ? this.workflowApi.removeChecklist(ctx.projectId, worklist.id)
      : this.workflowApi.removeWorklist(ctx.projectId, worklist.id);
    remove$.subscribe({
      next: () => {
        if (this.selectedWorklist()?.id === worklist.id) {
          this.selectedWorklist.set(null);
          this.records.set([]);
          this.recordsTotalCount.set(0);
        }
        this.loadWorklists();
        this.loadTabCounts();
      },
      error: () => this.notifications.error('Could not remove worklist.')
    });
  }

  protected formatWorklistDate(ts: string | number | null | undefined): string {
    if (!ts) {
      return '';
    }
    const formatted = formatEasternDate(ts);
    return formatted === 'n/a' ? String(ts) : formatted;
  }

  private buildApproveConceptRequest(overrideWarnings: boolean) {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.approvalActivityId());

    if (!projectId || !component?.id || !lastModified || !activityId) {
      return null;
    }

    return {
      activityId,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId
    };
  }

  private buildUpdateConceptPayload(): ContentComponentDetail | null {
    const component = this.selectedComponent();
    const workflowStatus = this.conceptUpdateWorkflowStatus().trim();

    if (!component?.id || !workflowStatus || !this.isConceptComponent(component)) {
      return null;
    }

    return {
      ...component,
      publishable: this.conceptUpdatePublishable(),
      workflowStatus
    };
  }

  private buildAddAtomRequest(
    overrideWarnings: boolean
  ): EditAddAtomRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.atomAddActivityId());
    const pendingAtom = overrideWarnings ? this.atomAddPendingAtom() : null;
    const termgroup = this.atomAddTermgroup().trim();
    const separatorIndex = termgroup.indexOf('/');
    const terminology =
      separatorIndex >= 0 ? termgroup.slice(0, separatorIndex) : '';
    const termType =
      separatorIndex >= 0 ? termgroup.slice(separatorIndex + 1) : '';
    const atom =
      pendingAtom ??
      {
        codeId: this.atomAddCodeId().trim(),
        conceptId: this.atomAddConceptId().trim(),
        descriptorId: this.atomAddDescriptorId().trim(),
        language: this.atomAddLanguage().trim(),
        name: this.atomAddName().trim(),
        publishable: true,
        termType,
        terminology,
        terminologyId: '',
        version: this.versionForTerminology(terminology),
        workflowStatus: this.atomAddStatus().trim()
      };

    if (
      !projectId ||
      !component?.id ||
      !atom.name ||
      !atom.termType ||
      !atom.terminology ||
      !atom.language ||
      (!atom.codeId && !atom.conceptId && !atom.descriptorId) ||
      !lastModified ||
      !activityId
    ) {
      return null;
    }

    return {
      activityId,
      atom,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId
    };
  }

  private buildRemoveAtomRequest(
    atom: ContentAtom,
    overrideWarnings: boolean
  ): EditRemoveAtomRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.atomRemovalActivityId());

    if (!projectId || !component?.id || !atom.id || !lastModified || !activityId) {
      return null;
    }

    return {
      activityId,
      atomId: atom.id,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId
    };
  }

  private buildMoveAtomsRequest(
    overrideWarnings: boolean
  ): EditMoveAtomsRequest | null {
    const pendingRequest = overrideWarnings ? this.atomMovePendingRequest() : null;
    if (pendingRequest) {
      return {
        ...pendingRequest,
        overrideWarnings: true
      };
    }

    const projectId = this.projectId();
    const component = this.selectedComponent();
    const conceptId2 = this.parsePositiveInteger(this.atomMoveTargetConceptId());
    const atomIds = this.selectedAtomMoveIds();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.atomMoveActivityId());

    if (
      !projectId ||
      !component?.id ||
      !conceptId2 ||
      !atomIds.length ||
      !lastModified ||
      !activityId
    ) {
      return null;
    }

    return {
      activityId,
      atomIds,
      conceptId: component.id,
      conceptId2,
      lastModified,
      overrideWarnings,
      projectId
    };
  }

  private buildSplitConceptRequest(
    overrideWarnings: boolean
  ): EditSplitConceptRequest | null {
    const pendingRequest = overrideWarnings ? this.atomSplitPendingRequest() : null;
    if (pendingRequest) {
      return {
        ...pendingRequest,
        overrideWarnings: true
      };
    }

    const projectId = this.projectId();
    const component = this.selectedComponent();
    const atomIds = this.selectedAtomSplitIds();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.atomSplitActivityId());
    const relationshipType = this.atomSplitRelationshipType().trim();

    if (
      !projectId ||
      !component?.id ||
      !atomIds.length ||
      !lastModified ||
      !activityId ||
      !relationshipType
    ) {
      return null;
    }

    return {
      activityId,
      atomIds,
      conceptId: component.id,
      copyRelationships: this.atomSplitCopyRelated(),
      copySemanticTypes: this.atomSplitCopyRelated(),
      lastModified,
      overrideWarnings,
      projectId,
      relationshipType
    };
  }

  private buildUpdateAtomStatusRequest(
    atom: ContentAtom,
    workflowStatus: string,
    overrideWarnings: boolean
  ): EditUpdateAtomRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.atomUpdateActivityId());
    const pendingAtom = overrideWarnings ? this.atomUpdatePendingAtom() : null;
    const updatedAtom =
      pendingAtom && pendingAtom.id === atom.id
        ? pendingAtom
        : { ...atom, workflowStatus };

    if (!projectId || !component?.id || !atom.id || !lastModified || !activityId) {
      return null;
    }

    return {
      activityId,
      atom: updatedAtom,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId
    };
  }

  private buildUpdateAtomRequest(
    atom: ContentAtom,
    publishable: boolean,
    overrideWarnings: boolean
  ): EditUpdateAtomRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.atomUpdateActivityId());
    const pendingAtom = overrideWarnings ? this.atomEditPendingAtom() : null;
    const updatedAtom =
      pendingAtom && pendingAtom.id === atom.id
        ? pendingAtom
        : { ...atom, publishable };

    if (!projectId || !component?.id || !atom.id || !lastModified || !activityId) {
      return null;
    }

    return {
      activityId,
      atom: updatedAtom,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId
    };
  }

  private atomSimpleEditHasChanges(atom: ContentAtom): boolean {
    return (
      this.atomSimpleEditName().trim() !== (atom.name || '') ||
      this.atomSimpleEditTermgroup().trim() !== this.atomTermgroup(atom) ||
      this.atomSimpleEditLanguage().trim() !== (atom.language || '') ||
      this.atomSimpleEditPublishable() !== (atom.publishable !== false) ||
      this.atomSimpleEditSuppressible() !== (atom.suppressible === true)
    );
  }

  private buildSimpleAtomPayload(atom: ContentAtom): ContentAtom | null {
    const termgroup = this.parseTermgroup(this.atomSimpleEditTermgroup());
    const name = this.atomSimpleEditName().trim();
    const language = this.atomSimpleEditLanguage().trim();

    if (!atom.id || !termgroup || !name || !language) {
      return null;
    }

    return {
      ...atom,
      language,
      name,
      publishable: this.atomSimpleEditPublishable(),
      suppressible: this.atomSimpleEditSuppressible(),
      termType: termgroup.termType,
      terminology: termgroup.terminology,
      version: atom.version || this.versionForTerminology(termgroup.terminology)
    };
  }

  private buildRemoveAttributeRequest(
    attribute: ContentAttribute,
    overrideWarnings: boolean
  ): EditRemoveAttributeRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.attributeRemovalActivityId());

    if (!projectId || !component?.id || !attribute.id || !lastModified || !activityId) {
      return null;
    }

    return {
      activityId,
      attributeId: attribute.id,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId
    };
  }

  private buildAddAttributeRequest(
    overrideWarnings: boolean
  ): EditAddAttributeRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.attributeAddActivityId());
    const attribute =
      overrideWarnings && this.attributeAddPendingAttribute()
        ? this.attributeAddPendingAttribute()
        : {
            name: this.attributeAddName().trim(),
            value: this.attributeAddValue().trim()
          };

    if (
      !projectId ||
      !component?.id ||
      !attribute?.name ||
      !attribute.value ||
      !lastModified ||
      !activityId
    ) {
      return null;
    }

    return {
      activityId,
      attribute,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId
    };
  }

  private buildAddSemanticTypeRequest(
    overrideWarnings: boolean
  ): EditAddSemanticTypeRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.semanticTypeAddActivityId());
    const semanticType = (
      overrideWarnings
        ? this.semanticTypeAddPendingValue() || this.semanticTypeAddValue()
        : this.semanticTypeAddValue()
    ).trim();

    if (!projectId || !component?.id || !semanticType || !lastModified || !activityId) {
      return null;
    }

    return {
      activityId,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId,
      semanticType
    };
  }

  private buildMergeConceptRequest(
    overrideWarnings: boolean
  ): EditMergeConceptRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const activityId = this.mutationActivityId(this.mergeActivityId());
    const targetConceptId = this.parsePositiveInteger(this.mergeTargetConceptId());
    const selectedTarget = this.selectedMergeTarget();
    const lastModified = this.mergeFromLastModifiedEpoch();

    if (!projectId || !component?.id || !targetConceptId || !lastModified || !activityId) {
      return null;
    }

    if (this.mergeReverseOrder()) {
      if (!selectedTarget?.id || selectedTarget.id !== targetConceptId) {
        return null;
      }

      return {
        activityId,
        conceptId: targetConceptId,
        conceptId2: component.id,
        lastModified,
        overrideWarnings,
        projectId
      };
    }

    return {
      activityId,
      conceptId: component.id,
      conceptId2: targetConceptId,
      lastModified,
      overrideWarnings,
      projectId
    };
  }

  private buildAddRelationshipRequest(
    overrideWarnings: boolean,
    relationshipType: string
  ): EditAddRelationshipRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.relationshipAddActivityId());
    const pendingRelationship = overrideWarnings
      ? this.relationshipAddPendingRelationship()
      : null;
    const targetConceptId =
      pendingRelationship?.toId ??
      this.parsePositiveInteger(this.relationshipAddTargetConceptId());
    const selectedTarget =
      targetConceptId && this.selectedRelationshipTarget()?.id === targetConceptId
        ? this.selectedRelationshipTarget()
        : null;
    const relationship =
      pendingRelationship ??
      (component && targetConceptId
        ? this.buildRelationshipPayload(component, targetConceptId, relationshipType, selectedTarget)
        : null);

    if (
      !projectId ||
      !component?.id ||
      !targetConceptId ||
      !relationship ||
      !relationship.relationshipType ||
      !relationship.terminology ||
      !relationship.version ||
      !lastModified ||
      !activityId
    ) {
      return null;
    }

    return {
      activityId,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId,
      relationship
    };
  }

  private buildAddRelationshipsRequest(
    overrideWarnings: boolean,
    relationshipType: string
  ): EditAddRelationshipsRequest | null {
    const pendingRelationships = overrideWarnings
      ? this.relationshipAddPendingRelationships()
      : null;
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.relationshipAddActivityId());
    const relationships =
      pendingRelationships ??
      (component
        ? this.selectedRelationshipTargets()
            .map((target) =>
              target.id
                ? this.buildRelationshipPayload(
                    component,
                    target.id,
                    relationshipType,
                    target
                  )
                : null
            )
            .filter(
              (relationship): relationship is ContentRelationship =>
                Boolean(relationship)
            )
        : []);

    if (
      !projectId ||
      !component?.id ||
      !relationships.length ||
      !relationshipType.trim() ||
      !lastModified ||
      !activityId
    ) {
      return null;
    }

    return {
      activityId,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId,
      relationships
    };
  }

  private buildRelationshipPayload(
    component: ContentComponentDetail,
    targetConceptId: number,
    relationshipType: string,
    selectedTarget: ContentSearchResult | null
  ): ContentRelationship {
    const terminology = component.terminology || this.terminology();
    const version =
      component.version || this.versionForTerminology(terminology || this.terminology());

    return {
      additionalRelationshipType: '',
      assertedDirection: false,
      fromId: component.id,
      fromName: component.name,
      fromTerminology: component.terminology,
      fromTerminologyId: component.terminologyId,
      fromVersion: component.version,
      group: null,
      hierarchical: false,
      inferred: false,
      name: null,
      obsolete: false,
      published: false,
      relationshipType: relationshipType.trim(),
      stated: false,
      suppressible: false,
      terminology,
      terminologyId: '',
      toId: targetConceptId,
      toName: selectedTarget?.name || selectedTarget?.value || '',
      toTerminology: selectedTarget?.terminology || terminology,
      toTerminologyId: selectedTarget?.terminologyId || '',
      toVersion: selectedTarget?.version || version,
      type: 'RELATIONSHIP',
      version,
      workflowStatus: 'NEEDS_REVIEW'
    };
  }

  private buildRemoveRelationshipRequest(
    relationship: ContentRelationship,
    overrideWarnings: boolean
  ): EditRemoveRelationshipRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.relationshipRemovalActivityId());

    if (
      !projectId ||
      !component?.id ||
      !relationship.id ||
      !lastModified ||
      !activityId
    ) {
      return null;
    }

    return {
      activityId,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId,
      relationshipId: relationship.id
    };
  }

  private buildRemoveSemanticTypeRequest(
    semanticType: ContentSemanticType,
    overrideWarnings: boolean
  ): EditRemoveSemanticTypeRequest | null {
    const projectId = this.projectId();
    const component = this.selectedComponent();
    const lastModified = this.selectedLastModifiedEpoch();
    const activityId = this.mutationActivityId(this.semanticTypeRemovalActivityId());

    if (!projectId || !component?.id || !semanticType.id || !lastModified || !activityId) {
      return null;
    }

    return {
      activityId,
      conceptId: component.id,
      lastModified,
      overrideWarnings,
      projectId,
      semanticTypeId: semanticType.id
    };
  }
}
