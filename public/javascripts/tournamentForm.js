$(function() {

  var $variant = $('#form3-variant');
  var $teamMember = $('#form3-conditions_teamMember_teamId');
  var $positionStandard = $('.form3 .position-standard');
  var $positionRussian = $('.form3 .position-russian');
  var $positionBrazilian = $('.form3 .position-brazilian');
  var $positionItalian = $('.form3 .position-italian');

  function showPosition() {
    $positionStandard.toggleNone($variant.val() == 1);
    $positionRussian.toggleNone($variant.val() == 11);
    $positionBrazilian.toggleNone($variant.val() == 12);
    $positionItalian.toggleNone($variant.val() == 13);
  };
  $variant.on('change', showPosition);
  showPosition();

  var $system = $('#form3-tournamentType_system');
  var $itaSwissFields = $('.ita-swiss-fields');
  function updateTournamentSystems() {
    if (!$system.length) return;
    var variantId = $variant.val();
    $system.find('option').each(function() {
      var allowedVariants = $(this).attr('data-variants');
      var allowed = !allowedVariants || allowedVariants.split(',').indexOf(variantId) !== -1;
      $(this).prop('disabled', !allowed).prop('hidden', !allowed);
    });
    if ($system.find('option:selected').prop('disabled')) {
      $system.val($system.find('option:not(:disabled)').first().val());
    }
    $itaSwissFields.toggle($system.val() === '2');
  }
  $variant.on('change', updateTournamentSystems);
  $system.on('change', updateTournamentSystems);
  updateTournamentSystems();

  function maxDate() {
    return new Date(Date.now() + 1000 * 3600 * 24 * ($teamMember.val() ? 180 : 31));
  }
  $teamMember.on('change', function() {
    if ($flatpickr) {
      $flatpickr.set({ maxDate: maxDate() });
    }
  })

  $('form .conditions a.show').on('click', function() {
    $(this).remove();
    $('form .conditions').addClass('visible');
  });

  if (!$("main div.crud").length) {
    var $flatpickr = $("main form .flatpickr").flatpickr({
      minDate: 'today',
      maxDate: maxDate(),
      dateFormat: 'Z',
      altInput: true,
      altFormat: 'Y-m-d h:i K'
    });
  }
});
