package uk.nhs.hee.tis.trainee.credentials.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementDataDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDataDto;

class CredentialDataMapperTest {

  private CredentialDataMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new CredentialDataMapperImpl();
  }

  @Test
  void shouldMapConstantsToProgrammeMembershipCredentialDto() {
    ProgrammeMembershipDataDto data = new ProgrammeMembershipDataDto("", "",
        LocalDate.MIN, LocalDate.MAX);

    ProgrammeMembershipCredentialDto dto = mapper.toCredential(data);

    assertThat("Unexpected origin", dto.metadataOrigin(),
        is(mapper.METADATA_ORIGIN_VALUE));
    assertThat("Unexpected assurance policy", dto.metadataAssurancePolicy(),
        is(mapper.METADATA_ASSURANCE_POLICY_VALUE));
    assertThat("Unexpected assurance outcome", dto.metadataAssuranceOutcome(),
        is(mapper.METADATA_ASSURANCE_OUTCOME_VALUE));
    assertThat("Unexpected provider", dto.metadataProvider(),
        is(mapper.METADATA_PROVIDER_VALUE));
    assertThat("Unexpected verifier", dto.metadataVerifier(),
        is(mapper.METADATA_VERIFIER_VALUE));
    assertThat("Unexpected verification method", dto.metadataVerificationMethod(),
        is(mapper.METADATA_VERIFICATION_METHOD_VALUE));
    assertThat("Unexpected pedigree", dto.metadataPedigree(),
        is(mapper.METADATA_PEDIGREE_VALUE));
    assertThat("Unexpected last refresh", dto.metadataLastRefresh(),
        is(LocalDate.now()));
  }

  @Test
  void shouldMapConstantsToPlacementCredentialDto() {
    PlacementDataDto data = new PlacementDataDto("", "", "", "",
        "", "", LocalDate.MIN, LocalDate.MAX);

    PlacementCredentialDto dto = mapper.toCredential(data);

    assertThat("Unexpected origin", dto.metadataOrigin(),
        is(mapper.METADATA_ORIGIN_VALUE));
    assertThat("Unexpected assurance policy", dto.metadataAssurancePolicy(),
        is(mapper.METADATA_ASSURANCE_POLICY_VALUE));
    assertThat("Unexpected assurance outcome", dto.metadataAssuranceOutcome(),
        is(mapper.METADATA_ASSURANCE_OUTCOME_VALUE));
    assertThat("Unexpected provider", dto.metadataProvider(),
        is(mapper.METADATA_PROVIDER_VALUE));
    assertThat("Unexpected verifier", dto.metadataVerifier(),
        is(mapper.METADATA_VERIFIER_VALUE));
    assertThat("Unexpected verification method", dto.metadataVerificationMethod(),
        is(mapper.METADATA_VERIFICATION_METHOD_VALUE));
    assertThat("Unexpected pedigree", dto.metadataPedigree(),
        is(mapper.METADATA_PEDIGREE_VALUE));
    assertThat("Unexpected last refresh", dto.metadataLastRefresh(),
        is(LocalDate.now()));
  }
}
